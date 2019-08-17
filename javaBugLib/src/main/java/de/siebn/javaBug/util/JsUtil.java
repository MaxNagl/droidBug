package de.siebn.javaBug.util;

public class JsUtil {

    public static String getSelector(XML xml) {
        String id = xml.getId();
        return id == null ? null : "#" + id;
    }

    public static String getJQuerySelector(XML xml) {
        String selector = getSelector(xml);
        return selector == null ? null : "$(\"" + selector + "\")";
    }

}
