package de.siebn.javaBug.objectOut;

import java.util.List;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 16.03.2015.
 */
public interface OutputCategory extends JavaBug.BugPlugin {
    void add(XML ul, Object o);

    BugElement get(Object o);

    String getId();

    String getType();

    String getName(Object o);

    boolean canOutputClass(Class<?> clazz);

    boolean opened(List<OutputCategory> others, boolean alreadyOpened);
}
