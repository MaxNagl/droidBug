package de.siebn.javaBug.plugins.scripts;

import org.mozilla.javascript.*;

import java.util.*;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.AllClassMembers;

public class BugScriptRhinoPlugin implements BugScriptEnginePlugin {
    private final JavaBug javaBug;

    public BugScriptRhinoPlugin(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    @Override
    public void getEngines(List<BugScriptEngine> engines, Set<String> loadedExtensions) {
        try {
            engines.add(new BugScriptEngineRhino());
        } catch (NoClassDefFoundError e) {
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

    class BugScriptEngineRhino implements BugScriptEngine {
        private final ThreadLocal<Context> contexts = new ThreadLocal<>();
        private ScriptableObject scope;

        public BugScriptEngineRhino() {
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
                            Object o = resolve(name);
                            return o != null || super.has(name, start);
                        }

                        @Override
                        public Object get(String name, Scriptable start) {
                            Object o = resolve(name);
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
        public Object eval(String script) {
            Context context = getContext();
            return context.evaluateString(scope, script, "console", 1, null);
        }

        @Override
        public String getName() {
            return "rhino";
        }

        @Override
        public String getNameShort() {
            return "js";
        }

        private Object resolve(String name) {
            Object o = javaBug.resolveReference(name);
            if (o == null) {
                if (AllClassMembers.topPackageExists(name)) {
                    o = eval("Packages." + name);
                }
            }
            return o;
        }
    }
}
