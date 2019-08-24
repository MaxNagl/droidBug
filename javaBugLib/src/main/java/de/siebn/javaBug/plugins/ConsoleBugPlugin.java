package de.siebn.javaBug.plugins;


import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

import javax.script.*;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.BugElement.BugInputList.Option;
import de.siebn.javaBug.NanoHTTPD.Response;
import de.siebn.javaBug.NanoHTTPD.Response.Status;
import de.siebn.javaBug.plugins.StreamBugPlugin.BugStream;
import de.siebn.javaBug.typeAdapter.TypeAdapters;

/**
 * Created by Sieben on 05.03.2015.
 */
public class ConsoleBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBug javaBug;
    public static String a = "abc";

    private List<ScriptEngine> scriptEngines;

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
        if ("a".equals(key)) return 1;
        if ("b".equals(key)) return 2;
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
        BugSplit split = new BugSplit(BugSplit.ORIENTATION_VERTICAL);

        BugPre stream = new BugElement.BugPre("");
        stream.setStyle("height", "100%");
        stream.stream = "/stream/";
        split.add(new BugSplitElement(stream));

        BugEntry entry = new BugEntry();
        BugInvokable invokable = new BugInvokable(null);
        invokable.url = "/exec/";
        List<ScriptEngine> scriptEngines = getScriptEngines();
        BugInputList engine = new BugInputList("engine", scriptEngines.get(0).getFactory().getEngineName());
        for (ScriptEngine e : scriptEngines) engine.options.add(new Option(e.getFactory().getEngineName(), e.getFactory().getEngineName()));
        invokable.add(engine);
        BugInputText script = new BugInputText("script", "");
        invokable.add(script);
        entry.add(invokable);
        split.add(new BugSplitElement(entry));

        return split;
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
            javaBug.getStreamBugPlugin().getConsoleStream().sendFormatedText("> " + TypeAdapters.toString(result + "\n"), BugFormat.value.clazzes);
        } catch (ScriptException e) {
            javaBug.getStreamBugPlugin().getConsoleStream().sendFormatedText(e.getMessage() + "\n", BugFormat.colorError.clazzes);
        }
        return "";
    }

    @Override
    public String getTabName() {
        return "Console";
    }

    @Override
    public Object getContent() {
        return "/console/";
    }

    @Override
    public int getOrder() {
        return -1000;
    }
}
