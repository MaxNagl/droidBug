package de.siebn.javaBug.plugins;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.NanoHTTPD;
import de.siebn.javaBug.util.XML;

import java.util.Set;

/**
 * Created by Sieben on 05.03.2015.
 */
public class ThreadsBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBug javaBug;

    public ThreadsBugPlugin(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    @JavaBug.Serve("^/threads/")
    public String serveThreads() {
        XML xhtml = new XML();
        XML ul = xhtml.add("ul");
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            ul.add("li").setClass("object").setAttr("expand", "/threadDetails/" + thread.getId()).add("span").setClass("value").appendText(thread.getName());
        }
        return xhtml.getHtml();
    }

    @JavaBug.Serve("^/threadDetails/([^/]*)")
    public String serveThreadDetails(String[] param) {
        int id = Integer.parseInt(param[1]);
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            if (thread.getId() == id) {
                XML ul = new XML("ul");
                ul.add("li").setClass("object").setAttr("expand", "!/stacktrace/" + id).appendText("Stacktrace");
                ul.add("li").setClass("object").setAttr("expand", javaBug.getObjectBug().getObjectDetailsLink(thread)).appendText("Object");
                return ul.getXml();
            }
        }
        throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Thread + \"" + id +"\" not found.");
    }

    @JavaBug.Serve("^/stacktrace/([^/]*)")
    public String serveStacktrace(String[] param) {
        int id = Integer.parseInt(param[1]);
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            if (thread.getId() == id) {
                XML ul = new XML("ul");
                for (StackTraceElement stacktrace : thread.getStackTrace())
                    ul.add("li").setClass("object").add("span").setClass("value").appendText(stacktrace.toString());
                return ul.getXml();
            }
        }
        throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Thread + \"" + id +"\" not found.");
    }

    @Override
    public String getTabName() {
        return "Threads";
    }

    @Override
    public String getUrl() {
        return "/threads/";
    }

    @Override
    public String getTagClass() {
        return "threads";
    }

    @Override
    public int getOrder() {
        return 2000;
    }
}
