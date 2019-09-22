package de.siebn.javaBug.android;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugImg;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.objectOut.BugAbstractOutputCategory;
import de.siebn.javaBug.objectOut.BugOutputCategory;

public class ViewShotOutput extends BugAbstractOutputCategory {
    private final boolean noChildren;

    public ViewShotOutput(JavaBugCore javaBug, boolean noChildren) {
        super(javaBug, "viewShot" + (noChildren ? "noChildren" : ""), "View Shot " + (noChildren ? " (no children)" : ""), 100);
        this.noChildren = noChildren;
    }

    @Override
    public BugElement get(Object o) {
        ViewBugPlugin viewBug = javaBug.getPlugin(ViewBugPlugin.class);
        return new BugImg().setSrc(viewBug.getLinkToViewShot((View) o, noChildren, false));
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        if (noChildren) return ViewGroup.class.isAssignableFrom(clazz);
        return View.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean opened(List<BugOutputCategory> others, boolean alreadyOpened) {
        return false;
    }
}
