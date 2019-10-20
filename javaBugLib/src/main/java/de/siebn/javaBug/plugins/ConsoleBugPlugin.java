package de.siebn.javaBug.plugins;


import java.util.Map;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.JavaBugCore.BugReferenceResolver;
import de.siebn.javaBug.plugins.scripts.BugScriptEnginePlugin.BugScriptEngine;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.BugObjectCache;

/**
 * Created by Sieben on 05.03.2015.
 */
public class ConsoleBugPlugin implements RootBugPlugin.MainBugPlugin, BugReferenceResolver {
    private final JavaBugCore javaBug;
    private ScriptConsole scriptConsole = new ScriptConsole();

    public ConsoleBugPlugin(JavaBugCore javaBug) {
        this.javaBug = javaBug;
    }

    @JavaBugCore.Serve("^/console/")
    public BugElement serveConsole() {
        BugSplit split = new BugSplit(BugSplit.ORIENTATION_VERTICAL);

        BugPre stream = new BugElement.BugPre("Welcome");
        stream.setStyle("height", "100%").addClazz("console");
        stream.stream = "/stream/console";
        split.add(new BugSplitElement(stream));

        BugInputText script = new BugInputText("script", "");
        script.mode = "script";
        script.nullable = false;
        script.textable = false;
        script.addClazz("console");

        BugInvokable invokable = new BugInvokable(null);
        invokable.url = "/exec/";
        invokable.add(script).addClazz("console");
        split.add(new BugSplitElement(invokable).setFixed("auto").setWeight("0"));

        return split;
    }

    @JavaBugCore.Serve("^/exec/")
    public Object exec(NanoHTTPD.IHTTPSession session) {
        Map<String, String> parms = session.getParms();
        String script = parms.get("script").trim();
        String mode = parms.get("script-type");
        BugScriptEngine scriptEngine = javaBug.getScriptBugPlugin().getScriptEngineMap().get(mode);

        BugList entry = new BugList();
        entry.add(new BugText((mode.startsWith("script-") ? mode.substring(7) : mode) + "> ").setStyle("vertical-align", "top").format(BugFormat.colorSecondary));
        entry.add(new BugText(script).setStyle("white-space", "pre-wrap").setStyle("display", "inline-block"));
        javaBug.getStreamBugPlugin().getConsoleStream().send(entry);

        try {
            Object result = scriptEngine.eval(script);
            if (result != null) scriptConsole.log(result, BugFormat.value);
        } catch (Throwable t) {
            t.printStackTrace();
            scriptConsole.log(t.getMessage(), BugFormat.colorError);
        }
        return "";
    }

    @Override
    public Object resolve(String reference) {
        if (reference.equals("console")) return scriptConsole;
        return null;
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
                entry.setExpandInclude(ObjectBugPlugin.getObjectDetailsLink(o)).setReference(BugObjectCache.getReference(o));
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
        return 1000;
    }
}
