package de.siebn.javaBug.objectOut;

import java.util.List;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.JavaBugCore;

/**
 * Created by Sieben on 16.03.2015.
 */
public interface OutputCategory extends JavaBugCore.BugPlugin {
    BugElement get(Object o);

    String getType();

    String getName(Object o);

    boolean canOutputClass(Class<?> clazz);

    boolean opened(List<OutputCategory> others, boolean alreadyOpened);
}
