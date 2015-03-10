package de.siebn.javaBug;

/**
 * Created by Sieben on 09.03.2015.
 */
public class HtmlPage {
    public static XML getDefaultPage() {
        XML xhtml = new XML();
        XML head = xhtml.add("head");
        XML body = xhtml.add("body");
        head.add("link").setAttr("rel", "stylesheet").setAttr("href", "//siebn.de/droidBug/test.css");
        head.add("script").setAttr("src", "//code.jquery.com/jquery-2.1.3.js");
        head.add("script").setAttr("src", "//siebn.de/droidBug/test.js");
        return xhtml;
    }
}
