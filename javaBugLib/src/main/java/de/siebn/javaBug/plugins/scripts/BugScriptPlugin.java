package de.siebn.javaBug.plugins.scripts;

import java.util.List;
import java.util.Set;

import de.siebn.javaBug.JavaBug.BugPlugin;

public interface BugScriptPlugin extends BugPlugin {
    void getEngines(List<BugScriptEngine> engines, Set<String> loadedExtensions);

    public interface BugScriptEngine {
        Object eval(String script) throws Throwable;

        String getName();
    }
}
