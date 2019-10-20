package de.siebn.javaBug.plugins.buggables;

import de.siebn.javaBug.JavaBugCore.BugPlugin;

public interface BuggablePlugin extends BugPlugin {
    boolean canBug(Object object);
    <T> T bug(Object id, String title, T object);
}
