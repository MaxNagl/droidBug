package de.siebn.javaBug.plugins;

import java.util.Set;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 05.03.2015.
 */
public class ThreadsBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBug javaBug;

    public ThreadsBugPlugin(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    @JavaBug.Serve("^/threadsJson/")
    public BugElement serveThreadsJson() {
        BugList list = new BugList();
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            BugEntry entry = new BugEntry();
            entry.add(new BugText(thread.getName()).format(BugFormat.title).setOnClick(BugElement.ON_CLICK_EXPAND).format(BugFormat.title));
            entry.setExpand(javaBug.getObjectBug().getObjectDetailsLinkJson(thread));
            list.add(entry);
        }
        return list;
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
        throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Thread + \"" + id + "\" not found.");
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
        throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Thread + \"" + id + "\" not found.");
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
    public String getContentUrl() {
        return "/threadsJson/";
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
