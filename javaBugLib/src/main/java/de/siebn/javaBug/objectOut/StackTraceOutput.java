package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 16.03.2015.
 */
public class StackTraceOutput extends AbstractOutputCategory {

    public StackTraceOutput(JavaBug javaBug) {
        super(javaBug, "stacktrace", "Stacktrace", 500);
    }

    @Override
    public BugElement get(Object o) {
        BugList list = new BugList();
        Thread t = (Thread) o;
        StackTraceElement[] stacktrace = t.getStackTrace();
        if (!t.isAlive() || stacktrace.length == 0) {
            BugEntry entry = new BugEntry();
            if (t.isAlive()) {
                entry.add(new BugText("Thread not alive.").format(BugFormat.colorError));
            } else {
                entry.add(new BugText("Error getting stacktrace.").format(BugFormat.colorError));
            }
            list.add(entry);
        } else {
            for (StackTraceElement ste : t.getStackTrace()) {
                BugEntry entry = new BugEntry();
                entry.add(new BugText(ste.getClassName()).format(BugFormat.clazz));
                entry.add(new BugText("."));
                entry.add(new BugText(ste.getMethodName()).format(BugFormat.method));
                entry.add(new BugText(UnicodeCharacters.NBSP + "("));
                if (ste.isNativeMethod()) {
                    entry.add(new BugText("Native Method").format(BugFormat.colorTernary));
                } else if (ste.getFileName() != null) {
                    entry.add(new BugText(ste.getFileName()).format(BugFormat.colorSecondary));
                    if (ste.getLineNumber() >= 0) {
                        entry.add(new BugText(":"));
                        entry.add(new BugText(String.valueOf(ste.getLineNumber())).format(BugFormat.colorSecondaryLight));
                    }
                } else {
                    entry.add(new BugText("Unknown Source").format(BugFormat.colorTernary));
                }
                entry.add(new BugText(")"));
                list.add(entry);
            }
        }
        return list;
    }

    @Override
    public void add(XML ul, Object o) {
        Thread t = (Thread) o;
        for (StackTraceElement stacktrace : t.getStackTrace())
            ul.add("li").setClass("object").add("span").setClass("value").appendText(stacktrace.toString());
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return Thread.class.isAssignableFrom(clazz);
    }
}
