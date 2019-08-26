package de.siebn.javaBug.plugins;


import org.mozilla.javascript.*;

import java.io.OutputStreamWriter;
import java.net.ContentHandlerFactory;
import java.util.*;

import javax.script.*;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.BugElement.BugInputList.Option;
import de.siebn.javaBug.plugins.ObjectBugPlugin.RootObject;
import de.siebn.javaBug.plugins.StreamBugPlugin.BugStream;
import de.siebn.javaBug.typeAdapter.TypeAdapters;

/**
 * Created by Sieben on 05.03.2015.
 */
public class ConsoleBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBug javaBug;
    private List<BugScriptEngine> scriptEngines;
    private ScriptConsole scriptConsole = new ScriptConsole();

    private Object getBinding(Object key) {
        if ("console".equals(key)) return scriptConsole;
        RootObject rootObject = javaBug.getObjectBug().getRootObjects().get(key);
        if (rootObject != null) return rootObject.value;
        return null;
    }

    public ConsoleBugPlugin(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    private synchronized List<BugScriptEngine> getScriptEngines() {
        if (scriptEngines == null) {
            scriptEngines = new ArrayList<>();
            HashSet<String> extensions = new HashSet<>();
            //loadJsr223Engines(scriptEngines, extensions);
            loadMozillaEngine(scriptEngines, extensions);
        }
        return scriptEngines;
    }

    private void loadJsr223Engines(List<BugScriptEngine> scriptEngines, Set<String> extensions) {
        try {
            for (ScriptEngineFactory scriptFactory : new ScriptEngineManager().getEngineFactories()) {
                extensions.addAll(scriptFactory.getExtensions());
                scriptEngines.add(new BugScriptEngineJsr223(scriptFactory));
            }
            if (!extensions.contains("kt") && !extensions.contains("kts")) {
                try {
                    ScriptEngineFactory factory = (ScriptEngineFactory) Class.forName("org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory").newInstance();
                    extensions.addAll(factory.getExtensions());
                    scriptEngines.add(new BugScriptEngineJsr223(factory));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (NoClassDefFoundError e) {
            // Android doesn't contain javax.script
        }
    }

    private void loadMozillaEngine(List<BugScriptEngine> scriptEngines, Set<String> extensions) {
        try {
            if (!extensions.contains("js")) {
                scriptEngines.add(new BugScriptEngineMozilla());
            }
        } catch (NoClassDefFoundError e) {
            // Android doesn't contain javax.script
        }
    }

    public abstract class BugScriptEngine {
        abstract Object eval(String script) throws Throwable;

        abstract String getName();
    }

    class BugScriptEngineJsr223 extends BugScriptEngine {
        private ScriptEngineFactory factory;
        private ScriptEngine engine;

        public BugScriptEngineJsr223(ScriptEngineFactory factory) {
            this.factory = factory;
            engine = factory.getScriptEngine();
            engine.setBindings(new SimpleBindings() {
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
            }, ScriptContext.GLOBAL_SCOPE);
            ScriptContext context = engine.getContext();
            BugStream consoleStream = javaBug.getStreamBugPlugin().getConsoleStream();
            context.setWriter(new OutputStreamWriter(consoleStream.createOutputStream()));
            context.setErrorWriter(new OutputStreamWriter(consoleStream.createOutputStreamWithClazz(BugFormat.colorError.clazzes)));
        }

        @Override
        Object eval(String script) throws Throwable {
            return engine.eval(script);
        }

        @Override
        String getName() {
            return factory.getEngineName();
        }
    }

    class BugScriptEngineMozilla extends BugScriptEngine {
        private final ThreadLocal<Context> contexts = new ThreadLocal<>();
        private ScriptableObject scope;

        public BugScriptEngineMozilla() {
            new ContextFactory(); // Check if class can be found.
        }

        Context getContext() {
            Context context = contexts.get();
            if (context == null) {
                context = new ContextFactory() {
                    @Override
                    protected boolean hasFeature(Context cx, int featureIndex) {
                        if (featureIndex == 13) return true;
                        return super.hasFeature(cx, featureIndex);
                    }
                }.enterContext();

                context.setOptimizationLevel(-1);
                if (scope == null) {
                    scope = new ScriptableObject() {
                        @Override
                        public String getClassName() {
                            return "JavaBug";
                        }

                        @Override
                        public boolean has(String name, Scriptable start) {
                            Object o = getBinding(name);
                            return o != null || super.has(name, start);
                        }

                        @Override
                        public Object get(String name, Scriptable start) {
                            Object o = getBinding(name);
                            return o != null ? o : super.get(name, start);
                        }
                    };
                    scope.setParentScope(context.initStandardObjects(null, true));
                }

                contexts.set(context);
            }
            return context;
        }

        @Override
        Object eval(String script) throws Throwable {
            Context context = getContext();
            return context.evaluateString(scope, script, "console", 1, null);
        }

        @Override
        String getName() {
            return "JavaScript";
        }
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
