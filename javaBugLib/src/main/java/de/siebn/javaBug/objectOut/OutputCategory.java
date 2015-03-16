package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 16.03.2015.
 */
public interface OutputCategory extends JavaBug.BugPlugin {
    public void add(XML ul, Object o);
    public String getType();
    public String getName();
    public boolean canOutputClass(Class<?> clazz);
    public boolean opened();
}
