package de.siebn.javaBug.testApplication;

import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.BugFormat;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.objectOut.AbstractOutputCategory;
import de.siebn.javaBug.objectOut.OutputMethod;
import de.siebn.javaBug.util.AllClassMembers;

public class BugFormatTest {
    @OutputMethod(value = "Formats", order = 100)
    public void formats(BugGroup list) {
        for (BugFormat bugFormat : BugFormat.values()) {
            BugEntry entry = new BugEntry();
            entry.add(new BugText("[Text]").format(bugFormat));
            entry.addSpace();
            entry.add(new BugLink("[Link]").format(bugFormat));
            entry.addSpace();
            entry.add(new BugInputText(null, "[BugInputText]").format(bugFormat));
            entry.addSpace();
            entry.add(new BugText(bugFormat.name()));
            list.add(entry);
        }
    }
}
