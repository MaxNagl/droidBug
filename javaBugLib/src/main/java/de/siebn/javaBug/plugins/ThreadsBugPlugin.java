package de.siebn.javaBug.plugins;

import java.util.Set;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;

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

    @Override
    public String getTabName() {
        return "Threads";
    }

    @Override
    public String getUrl() {
        return "/threadsJson/";
    }

    @Override
    public int getOrder() {
        return 2000;
    }
}
