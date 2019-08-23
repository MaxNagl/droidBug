package de.siebn.javaBug.android;

import android.view.View;

import java.util.List;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugImg;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.objectOut.AbstractOutputCategory;
import de.siebn.javaBug.objectOut.OutputCategory;
import de.siebn.javaBug.util.XML;

public class ViewShotOutput extends AbstractOutputCategory {
    public ViewShotOutput(JavaBug javaBug) {
        super(javaBug, "viewShow", "View Shot", 100);
    }

    @Override
    public BugElement get(Object o) {
        ViewBugPlugin viewBug = javaBug.getPlugin(ViewBugPlugin.class);
        return new BugImg().setSrc(viewBug.getLinkToViewShot((View) o));
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return View.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean opened(List<OutputCategory> others, boolean alreadyOpened) {
        return false;
    }
}
