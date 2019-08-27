package de.siebn.javaBug.plugins;


import java.util.*;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.BugElement.BugInputList.Option;
import de.siebn.javaBug.plugins.scripts.BugScriptPlugin;
import de.siebn.javaBug.plugins.scripts.BugScriptPlugin.BugScriptEngine;
import de.siebn.javaBug.typeAdapter.TypeAdapters;

/**
 * Created by Sieben on 05.03.2015.
 */
public class ConsoleBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBug javaBug;
    private List<BugScriptEngine> scriptEngines;
    private ScriptConsole scriptConsole = new ScriptConsole();

    public ConsoleBugPlugin(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    private synchronized List<BugScriptEngine> getScriptEngines() {
        if (scriptEngines == null) {
            scriptEngines = new ArrayList<>();
            HashSet<String> extensions = new HashSet<>();
            for (BugScriptPlugin plugin : javaBug.getPlugins(BugScriptPlugin.class)) {
                plugin.getEngines(scriptEngines, extensions);
            }
        }
        return scriptEngines;
    }

    @JavaBug.Serve("^/console/")
    public BugElement serveConsole() {
        BugInvokable invokable = new BugInvokable(null);
        invokable.url = "/exec/";
        invokable.addClazz("console");

        BugList list = new BugList();
        invokable.add(list);

        List<BugScriptEngine> scriptEngines = getScriptEngines();
        BugInputList engine = new BugInputList("engine", scriptEngines.isEmpty() ? "" : scriptEngines.get(0).getName());
        for (BugScriptEngine e : scriptEngines) engine.options.add(new Option(e.getName(), e.getName()));
        list.add(new BugDiv().add(engine));

        BugPre stream = new BugElement.BugPre("Welcome");
        stream.setStyle("height", "100%");
        stream.stream = "/stream/";
        list.add(new BugSplitElement(stream));

        BugInputText script = new BugInputText("script", "");
        script.mode = "script";
        script.nullable = true;
        list.add(new BugDiv().add(script));

        return invokable;
    }

    @JavaBug.Serve("^/exec/")
    public Object exec(NanoHTTPD.IHTTPSession session) {
        Map<String, String> parms = session.getParms();
        String engineName = parms.get("engine");
        String script = parms.get("script");

        BugScriptEngine scriptEngine = null;
        for (BugScriptEngine engine : getScriptEngines()) {
            if (engine.getName().equals(engineName)) {
                scriptEngine = engine;
                break;
            }
        }

        try {
            Object result = scriptEngine.eval(script);
            if (result != null) scriptConsole.log(result, BugFormat.value);
        } catch (Throwable t) {
            t.printStackTrace();
            scriptConsole.log(t.getMessage(), BugFormat.colorError);
        }
        return "";
    }

    public class ScriptConsole {
        public void log(Object o) {
            log(o, BugFormat.colorNeutralLight);
        }

        public void error(Object o) {
            log(o, BugFormat.colorError);
        }

        private void log(Object o, BugFormat format) {
            BugEntry entry = new BugEntry();
            entry.add(new BugText(o == null ? "null" : TypeAdapters.toString(o)).format(format).setOnClick(BugEntry.ON_CLICK_EXPAND));
            if (o != null && !o.getClass().isPrimitive() && !o.getClass().equals(String.class)) {
                entry.setExpandInclude(javaBug.getObjectBug().getObjectDetailsLink(o));
            }
            javaBug.getStreamBugPlugin().getConsoleStream().send(entry);
        }
    }

    @Override
    public String getTabName() {
        return "Console";
    }

    @Override
    public BugElement getContent() {
        return new BugInclude("/console/");
    }

    @Override
    public int getOrder() {
        return -1000;
    }
}
