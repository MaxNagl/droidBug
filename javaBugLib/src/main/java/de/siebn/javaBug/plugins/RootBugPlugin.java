package de.siebn.javaBug.plugins;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugTabs;
import de.siebn.javaBug.BugElement.BugTabs.BugTab;
import de.siebn.javaBug.BugElement.BugText;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.HtmlPage;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

import static de.siebn.javaBug.JavaBug.BugPlugin;

/**
 * Created by Sieben on 09.03.2015.
 */
public class RootBugPlugin implements BugPlugin {
    private final JavaBug jb;

    public RootBugPlugin(JavaBug jb) {
        this.jb = jb;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public interface MainBugPlugin extends BugPlugin {
        public String getTabName();
        public String getUrl();
    }

    @JavaBug.Serve("/root/")
    public BugElement serveRootJson() {
        BugTabs tabs = new BugTabs();
        for (MainBugPlugin plugin : jb.getPlugins(MainBugPlugin.class)) {
            BugTab tab = new BugTab();
            tab.title = plugin.getTabName();
            tab.content = plugin.getUrl();
            tabs.tabs.add(tab);
        }
        return tabs;
    }
}
