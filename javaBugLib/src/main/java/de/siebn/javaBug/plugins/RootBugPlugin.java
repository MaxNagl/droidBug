package de.siebn.javaBug.plugins;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.BugElement.BugTabs.BugTab;
import de.siebn.javaBug.util.XML;
import de.siebn.javaBug.util.XML.HTML;

import static de.siebn.javaBug.JavaBugCore.BugPlugin;

/**
 * Created by Sieben on 09.03.2015.
 */
public class RootBugPlugin implements BugPlugin {
    private final JavaBugCore jb;

    public RootBugPlugin(JavaBugCore jb) {
        this.jb = jb;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public interface MainBugPlugin extends BugPlugin {
        String getTabName();
        String getTabId();
        BugElement getContent();
    }

    @JavaBugCore.Serve("/")
    public XML serveRoot() {
        XML xhtml = new HTML();

        XML head = xhtml.add("head");
        head.add("link").setAttr("rel", "stylesheet/less").setAttr("href", "/file/bugStyle.less");
        head.add("script").setAttr("src", "/file/jquery.js");
        head.add("script").setAttr("src", "/file/less.js");
        head.add("script").setAttr("src", "http://10.7.1.23:7777/file/bugElements.js");
        head.add("script").appendText("$(function () { availableScripts = " + jb.getScriptBugPlugin().getEnginesJSArray() + "; $('body').loadContent('" + new BugInclude("/start/").toJson() + "', 'application/json'); });");

        XML body = xhtml.add("body");
        body.add("div").setId("loading").appendText("Loading...");

        return xhtml;
    }

    @JavaBugCore.Serve("/start/")
    public BugElement serveStart() {
        BugSplit split = new BugSplit(BugSplit.ORIENTATION_VERTICAL);
        BugTabs tabs = new BugTabs();
        split.add(new BugSplitElement(tabs).setWeight("4"));
        for (MainBugPlugin plugin : jb.getPlugins(MainBugPlugin.class)) {
            if (plugin instanceof ConsoleBugPlugin) {
                BugSplitElement resizeHandle = new BugSplitElement(null).setSplitType(BugSplitElement.TYPE_RESIZE_HANDLE).setFixed("auto").setWeight("0");
                resizeHandle.setContent(new BugList().add(new BugText("Console")));
                split.add(resizeHandle.format(BugFormat.resizeHandle));
                split.add(new BugSplitElement(plugin.getContent()));
            } else {
                BugTab tab = new BugTab();
                tab.setTitle(plugin.getTabName());
                tab.setContent(plugin.getContent());
                tab.setId(plugin.getTabId());
                tabs.tabs.add(tab);
            }
        }
        return split;
    }
}
