package de.siebn.javaBug.util;

import de.siebn.javaBug.util.XML;

import java.io.File;

/**
 * Created by Sieben on 09.03.2015.
 */
public class HtmlPage {
    public static XML getDefaultPage() {
        XML xhtml = new XML();
        XML head = xhtml.add("head");
        XML body = xhtml.add("body");

        head.add("link").setAttr("rel", "stylesheet").setAttr("href", "/file/" + new File("html/droidBug.css").getAbsolutePath());
        head.add("script").setAttr("src", "//code.jquery.com/jquery-2.1.3.js");
        head.add("script").setAttr("src", "/file/" + new File("html/droidBug.js").getAbsolutePath());

        return xhtml;
    }
}
