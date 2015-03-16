package de.siebn.javaBug.plugins;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.XML;

import java.util.Set;

/**
 * Created by Sieben on 05.03.2015.
 */
public class ClassPathBugPlugin implements RootBugPlugin.MainBugPlugin {
    @JavaBug.Serve("^/classPath/")
    public String serveThreads() {
        XML xhtml = new XML();
        XML ul = xhtml.add("ul");
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            ul.add("li").setClass("object").setAttr("expand", "/threadDetails/" + thread.getId()).add("span").setClass("value").appendText(thread.getName());
        }
        return xhtml.getHtml();
    }

    @Override
    public String getTabName() {
        return "ClassPath";
    }

    @Override
    public String getUrl() {
        return "/classPath/";
    }

    @Override
    public String getTagClass() {
        return "classPaths";
    }

    @Override
    public int getPriority() {
        return 3000;
    }
}
