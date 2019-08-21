package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.UnicodeCharacters;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 16.03.2015.
 */
public class StackTraceOutput extends AbstractOutputCategory {

    public StackTraceOutput(JavaBug javaBug) {
        super(javaBug, "stacktrace", "Stacktrace", 500);
    }

    @Override
    public void add(BugGroup list, Object o) {
        Thread t = (Thread) o;
        StackTraceElement[] stacktrace = t.getStackTrace();
        if (!t.isAlive() || stacktrace == null || stacktrace.length == 0) {
            BugExpandableEntry entry = new BugExpandableEntry();
            if (t.isAlive()) {
                entry.add(new BugText("Thread not alive.").colorError());
            } else {
                entry.add(new BugText("Error getting stacktrace.").colorError());
            }
            list.add(entry);
        } else {
            for (StackTraceElement ste : t.getStackTrace()) {
                BugExpandableEntry entry = new BugExpandableEntry();
                entry.add(new BugText(ste.getClassName()).colorPrimary());
                entry.add(new BugText("."));
                entry.add(new BugText(ste.getMethodName()).colorPrimaryLight());
                entry.add(new BugText(UnicodeCharacters.NBSP + "("));
                if (ste.isNativeMethod()) {
                    entry.add(new BugText("Native Method").colorSecondary());
                } else if (ste.getFileName() != null) {
                    entry.add(new BugText(ste.getFileName()).colorTernary());
                    if (ste.getLineNumber() >= 0) {
                        entry.add(new BugText(":"));
                        entry.add(new BugText(String.valueOf(ste.getLineNumber())).colorTernaryLight());
                    }
                } else {
                    entry.add(new BugText("Unknown Source").colorSecondaryLight());
                }
                entry.add(new BugText(")"));
                list.add(entry);
            }
        }
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
