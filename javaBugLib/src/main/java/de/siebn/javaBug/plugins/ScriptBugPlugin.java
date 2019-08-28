package de.siebn.javaBug.plugins;

import java.util.*;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.JavaBug.BugPlugin;
import de.siebn.javaBug.plugins.scripts.BugScriptEnginePlugin;
import de.siebn.javaBug.plugins.scripts.BugScriptEnginePlugin.BugScriptEngine;

public class ScriptBugPlugin implements BugPlugin {
    private final JavaBug javaBug;
    private List<BugScriptEngine> scriptEngines;
    private Map<String, BugScriptEngine> scriptEngineMap;

    public ScriptBugPlugin(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public synchronized List<BugScriptEngine> getScriptEngines() {
        if (scriptEngines == null) {
            scriptEngines = new ArrayList<>();
            HashSet<String> extensions = new HashSet<>();
            for (BugScriptEnginePlugin plugin : javaBug.getPlugins(BugScriptEnginePlugin.class)) {
                plugin.getEngines(scriptEngines, extensions);
            }
            Set<String> shortNames = new HashSet<>();
            Set<String> shortNameDuplicates = new HashSet<>();
            for (BugScriptEngine engine : scriptEngines) {
                String nameShort = engine.getNameShort();
                if (shortNames.contains(nameShort)) shortNameDuplicates.add(nameShort);
                shortNames.add(nameShort);
            }
            scriptEngineMap = new LinkedHashMap<>();
            for (BugScriptEngine engine : scriptEngines) {
                String name = engine.getNameShort();
                if (shortNameDuplicates.contains(name)) name += "(" + engine.getName() + ")";
                scriptEngineMap.put("script-" + name, engine);
            }
        }
        return scriptEngines;
    }

    public Map<String, BugScriptEngine> getScriptEngineMap() {
        if (scriptEngineMap == null) getScriptEngines();
        return scriptEngineMap;
    }

    public String getEnginesJSArray() {
        StringBuilder sb = new StringBuilder("[");
        for (String engine : getScriptEngineMap().keySet()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("'").append(engine).append("'");
        }
        return sb.append("]").toString();
    }
}
