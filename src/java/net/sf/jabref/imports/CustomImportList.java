/*
 Copyright (C) 2005 Andreas Rudert, based on CustomExportList by ??

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
*/
package net.sf.jabref.imports;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.TreeSet;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

/**
 * Collection of user defined custom export formats. 
 * 
 * <p>The collection can be stored and retrieved from Preferences. It is sorted by the default
 * order of {@link ImportFormat}.</p>
 */
public class CustomImportList extends TreeSet {

  /**
   * Structure with data for a custom importer 
   */
  public class Importer implements Comparable {
    
    private String name;
    private String className;
    private String basePath;
    
    public Importer() {
      super();
    }
    
    public Importer(String[] data) {
      super();
      this.name = data[0];
      this.className = data[1];
      this.basePath = data[2];
    }
    
    public String getName() {
      return this.name;
    }
    
    public void setName(String name) {
      this.name = name;
    }
    
    public String getClassName() {
      return this.className;
    }
    
    public void setClassName(String className) {
      this.className = className;
    }
    
    public void setBasePath(String basePath) {
      this.basePath = basePath;
    }
    
    public File getBasePath() {
      return new File(basePath);
    }
    
    public URL getBasePathUrl() throws MalformedURLException {
      return getBasePath().toURL();
    }
    
    public String[] getAsStringArray() {
      return new String[] {name, className, basePath};
    }
    
    public boolean equals(Object o) {
      return o != null && o instanceof Importer && this.getName().equals(((Importer)o).getName());
    }
    
    public int hashCode() {
      return name.hashCode();
    }
    
    public int compareTo(Object o) {
      return this.getName().compareTo( ((Importer)o).getName() );
    }
    
    public String toString() {
      return this.name;
    }
    
    public ImportFormat getInstance() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
      URLClassLoader cl = new URLClassLoader(new URL[] {getBasePathUrl()});
      Class clazz = Class.forName(className, true, cl);
      ImportFormat importFormat = (ImportFormat)clazz.newInstance();
      importFormat.setIsCustomImporter(true);
      return importFormat;
    }
  }
  
  private JabRefPreferences prefs;

  public CustomImportList(JabRefPreferences prefs) {
    super();
    this.prefs = prefs;
    readPrefs();
  }


  private void readPrefs() {
    int i=0;
    String[] s = null;
    while ((s = prefs.getStringArray("customImportFormat"+i)) != null) {
      super.add(new Importer(s));
      i++;
    }
  }

  public void addImporter(Importer customImporter) {
    super.add(customImporter);
  }

  public void store() {
    purgeAll();
    Importer[] importers = (Importer[])this.toArray(new Importer[]{});
    for (int i = 0; i < importers.length; i++) {
      Globals.prefs.putStringArray("customImportFormat"+i, importers[i].getAsStringArray());
    }
  }

  private void purgeAll() {
    for (int i = 0; Globals.prefs.getStringArray("customImportFormat"+i) != null; i++) {
      Globals.prefs.remove("customImportFormat"+i);
    }
  }

}
