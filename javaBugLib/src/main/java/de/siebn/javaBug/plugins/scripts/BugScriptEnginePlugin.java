package de.siebn.javaBug.plugins.scripts;

import java.util.List;
import java.util.Set;

import de.siebn.javaBug.JavaBugCore.BugPlugin;

public interface BugScriptEnginePlugin extends BugPlugin {
    void getEngines(List<BugScriptEngine> engines, Set<String> loadedExtensions);

    public interface BugScriptEngine {
        Object eval(String script) throws Throwable;

        String getNameShort();

        String getName();
    }
}
