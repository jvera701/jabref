package org.jabref.gui.externalfiles;

import java.nio.file.Files;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.ViewModelTreeCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class UnlinkedFilesDialogView extends BaseDialog<Void> {

    @FXML private TextField directoryPathField;
    @FXML private ComboBox<FileChooser.ExtensionFilter> fileTypeSelection;
    @FXML private TreeView<FileNodeWrapper> tree;
    @FXML private Button buttonScan;
    @FXML private ButtonType importButton;
    @FXML private Button buttonExport;
    @FXML private Label progressText;
    @FXML private ProgressIndicator progressDisplay;
    @FXML private VBox progressPane;

    @FXML private TableView<ImportFilesResultItemViewModel> tvResult;
    @FXML private TableColumn<ImportFilesResultItemViewModel, JabRefIcon> colStatus;
    @FXML private TableColumn<ImportFilesResultItemViewModel, String> colMessage;
    @FXML private TableColumn<ImportFilesResultItemViewModel, String> colFile;

    @FXML private TitledPane filePane;
    @FXML private TitledPane resultPane;

    @Inject private PreferencesService preferencesService;
    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;
    @Inject private UndoManager undoManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private FileUpdateMonitor fileUpdateMonitor;

    private UnlinkedFilesDialogViewModel viewModel;

    public UnlinkedFilesDialogView() {
        this.setTitle(Localization.lang("Search for unlinked local files"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        Button btnImport = (Button) this.getDialogPane().lookupButton(importButton);
        ControlHelper.setAction(importButton, getDialogPane(), evt-> viewModel.startImport());
        btnImport.disableProperty().bindBidirectional(viewModel.applyButtonDisabled());
        btnImport.setTooltip(new Tooltip(Localization.lang("Starts the import of BibTeX entries.")));

        setResultConverter(button -> {
            if (button == ButtonType.CANCEL) {
                viewModel.cancelTasks();
            }
            return null;
        });
    }

    @FXML
    private void initialize() {

        viewModel = new UnlinkedFilesDialogViewModel(dialogService, ExternalFileTypes.getInstance(), undoManager, fileUpdateMonitor, preferencesService, stateManager, taskExecutor);
        viewModel.directoryPath().bindBidirectional(directoryPathField.textProperty());

        fileTypeSelection.setItems(FXCollections.observableArrayList(viewModel.getFileFilters()));
        new ViewModelListCellFactory<FileChooser.ExtensionFilter>()
        .withText(fileFilter -> fileFilter.getDescription() + fileFilter.getExtensions().stream().collect(Collectors.joining(", ", " (", ")")))
        .withIcon(fileFilter -> ExternalFileTypes.getInstance().getExternalFileTypeByExt(fileFilter.getExtensions().get(0))
                                                 .map(ExternalFileType::getIcon)
                                                 .orElse(null))
        .install(fileTypeSelection);

        new ViewModelTreeCellFactory<FileNodeWrapper>()
                .withText(node -> {
                    if (Files.isRegularFile(node.path)) {
                        // File
                        return node.path.getFileName().toString();
                    } else {
                        // Directory
                        return node.path.getFileName() + " (" + node.fileCount + " file" + (node.fileCount > 1 ? "s" : "") + ")";
                    }
                })
                .install(tree);

       tree.setPrefWidth(Double.POSITIVE_INFINITY);
       viewModel.treeRoot().bindBidirectional(tree.rootProperty());
       viewModel.scanButtonDisabled().bindBidirectional(buttonScan.disableProperty());
       viewModel.scanButtonDefaultButton().bindBidirectional(buttonScan.defaultButtonProperty());
       viewModel.exportButtonDisabled().bindBidirectional(buttonExport.disableProperty());
       viewModel.selectedExtension().bind(fileTypeSelection.valueProperty());

       tvResult.setItems(viewModel.resultTableItems());

       progressDisplay.progressProperty().bind(viewModel.progress());
       progressText.textProperty().bind(viewModel.progressText());

       progressPane.managedProperty().bind(viewModel.searchProgressVisible());
       progressPane.visibleProperty().bind(viewModel.searchProgressVisible());

       viewModel.filePaneExpanded().bindBidirectional(filePane.expandedProperty());
       viewModel.resultPaneExpanded().bindBidirectional(resultPane.expandedProperty());

       viewModel.scanButtonDefaultButton().setValue(true);
       viewModel.scanButtonDisabled().setValue(true);
       viewModel.applyButtonDisabled().setValue(true);
       fileTypeSelection.getSelectionModel().selectFirst();

       setupResultTable();

    }

    private void setupResultTable() {

        colFile.setCellValueFactory(cellData -> cellData.getValue().file());
        new ValueTableCellFactory<ImportFilesResultItemViewModel, String>()
        .withText(item -> item).withTooltip(item -> item)
        .install(colFile);

        colMessage.setCellValueFactory(cellData -> cellData.getValue().message());
        new ValueTableCellFactory<ImportFilesResultItemViewModel, String>()
        .withText(item->item).withTooltip(item->item)
        .install(colMessage);

        colStatus.setCellValueFactory(cellData -> cellData.getValue().getIcon());

        colStatus.setCellFactory(new ValueTableCellFactory<ImportFilesResultItemViewModel, JabRefIcon>().withGraphic(item -> {
            if (item == IconTheme.JabRefIcons.CHECK) {
                item = item.withColor(Color.GREEN);
            }
            if (item == IconTheme.JabRefIcons.WARNING) {
                item = item.withColor(Color.RED);
            }
            return item.getGraphicNode();
        }));

        tvResult.setColumnResizePolicy((param) -> true);
    }

    @FXML
    void browseFileDirectory(ActionEvent event) {
        viewModel.browseFileDirectory();
    }

    @FXML
    void collapseAll(ActionEvent event) {
        viewModel.collapseAll();
    }

    @FXML
    void expandAll(ActionEvent event) {
        viewModel.expandAll();
    }

    @FXML
    void scanFiles(ActionEvent event) {
        viewModel.startSearch();
    }

    @FXML
    void selectAll(ActionEvent event) {
        viewModel.selectAll();
    }

    @FXML
    void unselectAll(ActionEvent event) {
        viewModel.unselectAll();
    }

    @FXML
    void exportSelected(ActionEvent event) {
       viewModel.startExport();
    }

}