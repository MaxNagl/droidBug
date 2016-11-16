package de.siebn.javaBug.android;

import android.view.View;
import android.view.ViewGroup.LayoutParams;

import java.util.List;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.objectOut.AbstractOutputCategory;
import de.siebn.javaBug.objectOut.OutputCategory;
import de.siebn.javaBug.util.XML;

public class ViewOutput extends AbstractOutputCategory {
    public ViewOutput(JavaBug javaBug) {
        super(javaBug, "viewProperties", "View", 100);
    }

    @Property("width")
    public int width(View view, Integer value, boolean set) {
        if (set && value != null) {
            LayoutParams lp = view.getLayoutParams();
            lp.width = value;
            view.setLayoutParams(lp);
        }
        return view.getWidth();
    }

    @Property("height")
    public int height(View view, Integer value, boolean set) {
        if (set && value != null) {
            LayoutParams lp = view.getLayoutParams();
            lp.height = value;
            view.setLayoutParams(lp);
        }
        return view.getHeight();
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
