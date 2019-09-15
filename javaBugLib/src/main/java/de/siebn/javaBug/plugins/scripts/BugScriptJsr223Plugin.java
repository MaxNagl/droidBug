package de.siebn.javaBug.plugins.scripts;

import java.io.OutputStreamWriter;
import java.util.*;

import javax.script.*;

import de.siebn.javaBug.BugFormat;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.plugins.StreamBugPlugin.BugStream;
import de.siebn.javaBug.util.AllClassMembers;

public class BugScriptJsr223Plugin implements BugScriptEnginePlugin {
    private final static String[] DEFAULT_FACTORY_CLASSES = {
            "jdk.nashorn.api.scripting.NashornScriptEngineFactory",
            "org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory"
    };

    private final JavaBugCore javaBug;

    public BugScriptJsr223Plugin(JavaBugCore javaBug) {
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
        private boolean isJs;

        public BugScriptEngineJsr223(ScriptEngineFactory factory) {
            this.factory = factory;
            engine = factory.getScriptEngine();
            engine.setBindings(new SimpleBindings() {
                @Override
                public boolean containsKey(Object key) {
                    Object o = resolve(key);
                    return o != null || super.containsKey(key);
                }

                @Override
                public Object get(Object key) {
                    Object o = resolve(key);
                    return o != null ? o : super.get(key);
                }
            }, ScriptContext.GLOBAL_SCOPE);
            ScriptContext context = engine.getContext();
            BugStream consoleStream = javaBug.getStreamBugPlugin().getConsoleStream();
            context.setWriter(new OutputStreamWriter(consoleStream.createOutputStream()));
            context.setErrorWriter(new OutputStreamWriter(consoleStream.createOutputStreamWithClazz(BugFormat.colorError.clazzes)));
            isJs = factory.getExtensions().contains("js");
        }

        @Override
        public Object eval(String script) throws Throwable {
            return engine.eval(script);
        }

        @Override
        public String getName() {
            return factory.getEngineName();
        }

        @Override
        public String getNameShort() {
            List<String> extensions = factory.getExtensions();
            if (extensions == null || extensions.isEmpty()) return factory.getEngineName();
            return extensions.get(0);
        }

        private Object resolve(Object key) {
            if (!(key instanceof String)) return null;
            String name = (String) key;
            Object o = javaBug.resolveReference(name);
            if (o == null && isJs) {
                if (AllClassMembers.topPackageExists(name)) {
                    try {
                        o = eval("Packages." + name);
                    } catch (Throwable ignore) {
                    }
                }
            }
            return o;
        }
    }
}
