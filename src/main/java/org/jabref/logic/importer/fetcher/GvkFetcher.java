package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformators.AbstractQueryTransformer;
import org.jabref.logic.importer.fetcher.transformators.GVKQueryTransformer;
import org.jabref.logic.importer.fileformat.GvkParser;

import org.apache.http.client.utils.URIBuilder;

public class GvkFetcher implements SearchBasedParserFetcher {

    private static final String URL_PATTERN = "http://sru.gbv.de/gvk?";

    /**
     * Searchkeys are used to specify a search request. For example "tit" stands for "title".
     * If no searchkey is used, the default searchkey "all" is used.
     */
    private final Collection<String> searchKeys = Arrays.asList("all", "tit", "per", "thm", "slw", "txt", "num", "kon", "ppn", "bkl", "erj");

    @Override
    public String getName() {
        return "GVK";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_GVK);
    }

    @Override
    public URL getURLForQuery(String transformedQuery, AbstractQueryTransformer transformer) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
        uriBuilder.addParameter("version", "1.1");
        uriBuilder.addParameter("operation", "searchRetrieve");
        uriBuilder.addParameter("query", transformedQuery);
        uriBuilder.addParameter("maximumRecords", "50");
        uriBuilder.addParameter("recordSchema", "picaxml");
        uriBuilder.addParameter("sortKeys", "Year,,1");
        System.out.println(uriBuilder.build().toURL());
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new GvkParser();
    }

    @Override
    public AbstractQueryTransformer getQueryTransformer() {
        return new GVKQueryTransformer();
    }
}
