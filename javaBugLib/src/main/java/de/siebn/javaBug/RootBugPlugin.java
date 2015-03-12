package de.siebn.javaBug;

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
    public int getPriority() {
        return 0;
    }

    public interface MainBugPlugin extends BugPlugin {
        public String getTabName();
        public String getUrl();
        public String getTagClass();
    }

    @JavaBug.Serve("/")
    public String serveRoot() {
        XML page = HtmlPage.getDefaultPage();
        XML body = page.getFirstByTag("body");
        body.add("h1").appendText("Test");
        XML ul = body.add("ul").setClass("tabs");

        for (MainBugPlugin plugin : jb.getPlugins(MainBugPlugin.class)) {
            ul.add("li").setAttr("tabContent", plugin.getTagClass()).appendText(plugin.getTabName());
            body.add("div").setClass("tabContent " + plugin.getTagClass()).setAttr("autoLoad", plugin.getUrl());
        }
        return page.getHtml();
    }
}
