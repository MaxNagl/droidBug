package de.siebn.javaBug.android;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugImg;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.objectOut.AbstractOutputCategory;
import de.siebn.javaBug.objectOut.OutputCategory;
import de.siebn.javaBug.util.XML;

public class ViewShotOutput extends AbstractOutputCategory {
    private final boolean noChildren;

    public ViewShotOutput(JavaBug javaBug, boolean noChildren) {
        super(javaBug, "viewShot" + (noChildren ? "noChildren" : ""), "View Shot " + (noChildren ? " (no children)" : ""), 100);
        this.noChildren = noChildren;
    }

    @Override
    public BugElement get(Object o) {
        ViewBugPlugin viewBug = javaBug.getPlugin(ViewBugPlugin.class);
        return new BugImg().setSrc(viewBug.getLinkToViewShot((View) o) + (noChildren ? "?noChildren" : ""));
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        if (noChildren) return ViewGroup.class.isAssignableFrom(clazz);
        return View.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean opened(List<OutputCategory> others, boolean alreadyOpened) {
        return false;
    }
}
