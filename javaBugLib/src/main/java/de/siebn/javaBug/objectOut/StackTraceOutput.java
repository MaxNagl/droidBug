package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 16.03.2015.
 */
public class StackTraceOutput extends AbstractOutputCategory {

    public StackTraceOutput(JavaBug javaBug) {
        super(javaBug, "stacktrace", "Stacktract", 500);
    }

    @Override
    public void add(XML ul, Object o) {
        Thread t = (Thread) o;
        for (StackTraceElement stacktrace : t.getStackTrace())
            ul.add("li").setClass("object").add("span").setClass("value").appendText(stacktrace.toString());
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return clazz.isAssignableFrom(Thread.class);
    }
}
