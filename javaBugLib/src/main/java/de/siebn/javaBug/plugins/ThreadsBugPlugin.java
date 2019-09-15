package de.siebn.javaBug.plugins;

import java.util.Set;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;

/**
 * Created by Sieben on 05.03.2015.
 */
public class ThreadsBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBugCore javaBug;

    public ThreadsBugPlugin(JavaBugCore javaBug) {
        this.javaBug = javaBug;
    }

    @JavaBugCore.Serve("^/threads/")
    public BugElement serveThreadsJson() {
        BugList list = new BugList();
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            BugEntry entry = new BugEntry();
            entry.add(new BugText(thread.getName()).format(BugFormat.title).setOnClick(BugElement.ON_CLICK_EXPAND).format(BugFormat.title));
            entry.setExpandInclude(javaBug.getObjectBug().getObjectDetailsLink(thread));
            list.add(entry);
        }
        return list;
    }

    @Override
    public String getTabName() {
        return "Threads";
    }

    @Override
    public BugElement getContent() {
        return new BugDiv().add(new BugInclude("/threads/")).format(BugFormat.tabContent);
    }

    @Override
    public int getOrder() {
        return 2000;
    }
}
