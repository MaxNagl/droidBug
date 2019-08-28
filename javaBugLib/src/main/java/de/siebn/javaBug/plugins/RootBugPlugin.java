package de.siebn.javaBug.plugins;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.BugElement.BugTabs.BugTab;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.NanoHTTPD.Response;
import de.siebn.javaBug.util.HtmlPage;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;
import de.siebn.javaBug.util.XML.HTML;

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
        String getTabName();
        BugElement getContent();
    }

    @JavaBug.Serve("/")
    public XML serveRoot() {
        XML xhtml = new HTML();

        XML head = xhtml.add("head");
        head.add("link").setAttr("rel", "stylesheet/less").setAttr("href", "/file/bugStyle.less");
        head.add("script").setAttr("src", "/file/jquery.js");
        head.add("script").setAttr("src", "/file/less.js");
        head.add("script").setAttr("src", "/file/bugElements.js");
        head.add("script").appendText("$(function () { availableScripts = " + jb.getScriptBugPlugin().getEnginesJSArray() + "; $('body').loadContent('" + new BugInclude("/start/").toJson() + "', 'application/json'); });");

        XML body = xhtml.add("body");
        body.add("div").setId("loading").appendText("Loading...");

        return xhtml;
    }

    @JavaBug.Serve("/start/")
    public BugElement serveStart() {
        BugTabs tabs = new BugTabs();
        for (MainBugPlugin plugin : jb.getPlugins(MainBugPlugin.class)) {
            BugTab tab = new BugTab();
            tab.title = plugin.getTabName();
            tab.content = plugin.getContent();
            tabs.tabs.add(tab);
        }
        return tabs;
    }
}
