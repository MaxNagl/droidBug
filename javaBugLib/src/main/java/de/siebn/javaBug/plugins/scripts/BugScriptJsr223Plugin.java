package de.siebn.javaBug.plugins.scripts;

import java.io.OutputStreamWriter;
import java.util.*;

import javax.script.*;

import de.siebn.javaBug.BugFormat;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.plugins.StreamBugPlugin.BugStream;

public class BugScriptJsr223Plugin implements BugScriptPlugin {
    private final static String[] DEFAULT_FACTORY_CLASSES = {
            "jdk.nashorn.api.scripting.NashornScriptEngineFactory",
            "org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory"
    };

    private final JavaBug javaBug;

    public BugScriptJsr223Plugin(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    @Override
    public void getEngines(List<BugScriptEngine> engines, Set<String> loadedExtensions) {
        try {
            List<ScriptEngineFactory> engineFactories = new ArrayList<>(new ScriptEngineManager().getEngineFactories());
            for (String factories : DEFAULT_FACTORY_CLASSES) {
                try {
                    engineFactories.add((ScriptEngineFactory) Class.forName(factories).newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            HashSet<String> loaded = new HashSet<>();
            for (ScriptEngineFactory scriptFactory : engineFactories) {
                if (loaded.add(scriptFactory.getEngineName())) {
                    loadedExtensions.addAll(scriptFactory.getExtensions());
                    engines.add(new BugScriptEngineJsr223(scriptFactory));
                }
            }
        } catch (NoClassDefFoundError e) {
            // Android doesn't contain javax.script
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public class BugScriptEngineJsr223 implements BugScriptEngine {
        private ScriptEngineFactory factory;
        private ScriptEngine engine;

        public BugScriptEngineJsr223(ScriptEngineFactory factory) {
            this.factory = factory;
            engine = factory.getScriptEngine();
            engine.setBindings(new SimpleBindings() {
                @Override
                public boolean containsKey(Object key) {
                    System.out.println("ASDF " + key);
                    Object o = javaBug.getBinding(key);
                    return o != null || super.containsKey(key);
                }

                @Override
                public Object get(Object key) {
                    Object o = javaBug.getBinding(key);
                    return o != null ? o : super.get(key);
                }
            }, ScriptContext.GLOBAL_SCOPE);
            ScriptContext context = engine.getContext();
            BugStream consoleStream = javaBug.getStreamBugPlugin().getConsoleStream();
            context.setWriter(new OutputStreamWriter(consoleStream.createOutputStream()));
            context.setErrorWriter(new OutputStreamWriter(consoleStream.createOutputStreamWithClazz(BugFormat.colorError.clazzes)));
        }

        @Override
        public Object eval(String script) throws Throwable {
            return engine.eval(script);
        }

        @Override
        public String getName() {
            return factory.getEngineName();
        }
    }
}
