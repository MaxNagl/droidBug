package de.siebn.javaBug.plugins.scripts;

import java.util.List;
import java.util.Set;

import de.siebn.javaBug.JavaBug;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class BugScriptGroovyPlugin implements BugScriptEnginePlugin {
    private final JavaBug javaBug;

    public BugScriptGroovyPlugin(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    @Override
    public void getEngines(List<BugScriptEngine> engines, Set<String> loadedExtensions) {
        try {
            if (!loadedExtensions.contains("groovy")) {
                engines.add(new BugScriptEngineGroovy());
            }
        } catch (NoClassDefFoundError e) {
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

    class BugScriptEngineGroovy implements BugScriptEngine {
        private GroovyShell shell;

        public BugScriptEngineGroovy() {
            shell = new GroovyShell(new Binding() {
                @Override
                public Object getVariable(String name) {
                    Object o = javaBug.getBinding(name);
                    return o != null ? o : super.getVariable(name);
                }

                @Override
                public boolean hasVariable(String name) {
                    Object o = javaBug.getBinding(name);
                    return o != null || super.hasVariable(name);
                }
            });
        }

        @Override
        public Object eval(String script) {
            return shell.evaluate(script);
        }

        @Override
        public String getName() {
            return "Groovy Scripting Engine";
        }

        @Override
        public String getNameShort() {
            return "groovy";
        }
    }
}
