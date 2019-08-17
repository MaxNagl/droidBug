package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.JsonBugList;
import de.siebn.javaBug.util.XML;

import java.util.List;

/**
 * Created by Sieben on 16.03.2015.
 */
public interface OutputCategory extends JavaBug.BugPlugin {
    void add(XML ul, Object o);
    void add(JsonBugList list, Object o);
    String getId();
    String getType();
    String getName(Object o);
    boolean canOutputClass(Class<?> clazz);
    boolean opened(List<OutputCategory> others, boolean alreadyOpened);
}
