package de.siebn.javaBug.plugins;


import java.io.OutputStreamWriter;
import java.util.*;

import javax.script.*;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.BugElement.BugInputList.Option;
import de.siebn.javaBug.plugins.ObjectBugPlugin.RootObject;
import de.siebn.javaBug.plugins.StreamBugPlugin.BugStream;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.UnicodeCharacters;

/**
 * Created by Sieben on 05.03.2015.
 */
public class ConsoleBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBug javaBug;
    private List<ScriptEngine> scriptEngines;
    private ScriptConsole scriptConsole = new ScriptConsole();

    private Bindings bindings = new SimpleBindings() {
        @Override
        public boolean containsKey(Object key) {
            Object o = getBinding(key);
            return o != null || super.containsKey(key);
        }

        @Override
        public Object get(Object key) {
            Object o = getBinding(key);
            return o != null ? o : super.get(key);
        }
    };

    private Object getBinding(Object key) {
        if ("console".equals(key)) return scriptConsole;
        RootObject rootObject = javaBug.getObjectBug().getRootObjects().get(key);
        if (rootObject != null) return rootObject.value;
        return null;
    }

    public ConsoleBugPlugin(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    private synchronized List<ScriptEngine> getScriptEngines() {
        if (scriptEngines == null) {
            scriptEngines = new ArrayList<>();
            for (ScriptEngineFactory scriptFactory : new ScriptEngineManager().getEngineFactories()) {
                ScriptEngine scriptEngine = scriptFactory.getScriptEngine();
                scriptEngine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
                ScriptContext context = scriptEngine.getContext();
                BugStream consoleStream = javaBug.getStreamBugPlugin().getConsoleStream();
                context.setWriter(new OutputStreamWriter(consoleStream.createOutputStream()));
                context.setErrorWriter(new OutputStreamWriter(consoleStream.createOutputStreamWithClazz(BugFormat.colorError.clazzes)));
                scriptEngines.add(scriptEngine);
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

        List<ScriptEngine> scriptEngines = getScriptEngines();
        BugInputList engine = new BugInputList("engine", scriptEngines.get(0).getFactory().getEngineName());
        for (ScriptEngine e : scriptEngines) engine.options.add(new Option(e.getFactory().getEngineName(), e.getFactory().getEngineName()));
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

        ScriptEngine scriptEngine = null;
        for (ScriptEngine engine : getScriptEngines()) {
            if (engine.getFactory().getEngineName().equals(engineName)) {
                scriptEngine = engine;
                break;
            }
        }

        try {
            Object result = scriptEngine.eval(script);
            if (result != null) scriptConsole.log(result, BugFormat.value);
        } catch (ScriptException e) {
            scriptConsole.log(e.getMessage(), BugFormat.colorError);
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
            if (!o.getClass().isPrimitive() && !o.getClass().equals(String.class)) {
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
