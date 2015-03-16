package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.util.XML;

import java.util.List;

/**
 * Created by Sieben on 16.03.2015.
 */
public class StackTraceOutput implements OutputCategory {
    @Override
    public void add(XML ul, Object o) {
        Thread t = (Thread) o;
        for (StackTraceElement stacktrace : t.getStackTrace())
            ul.add("li").setClass("object").add("span").setClass("value").appendText(stacktrace.toString());
    }

    @Override
    public String getType() {
        return "stacktrace";
    }

    @Override
    public String getName() {
        return "Stacktrace";
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return clazz.isAssignableFrom(Thread.class);
    }

    @Override
    public boolean opened(List<OutputCategory> others, boolean alreadyOpened) {
        return true;
    }

    @Override
    public int getOrder() {
        return 500;
    }
}
