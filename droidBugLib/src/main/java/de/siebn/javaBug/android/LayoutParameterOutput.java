package de.siebn.javaBug.android;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;

import java.lang.reflect.Method;

import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.android.TypeAdapters.DpDimensionAdapter;
import de.siebn.javaBug.android.TypeAdapters.LayoutSizeDimensionAdapter;
import de.siebn.javaBug.android.TypeAdapters.PxDimensionAdapter;
import de.siebn.javaBug.objectOut.AbstractOutputCategory;

public class LayoutParameterOutput extends AbstractOutputCategory {
    public LayoutParameterOutput(JavaBugCore javaBug) {
        super(javaBug, "layoutparams", "Layout Params", 100);
    }

    @Property(value = "width", typeAdapters = {PxDimensionAdapter.class, DpDimensionAdapter.class, LayoutSizeDimensionAdapter.class})
    public int width(View view, Integer value, boolean set) {
        LayoutParams lp = view.getLayoutParams();
        if (set && value != null) {
            lp.width = value;
            view.setLayoutParams(lp);
        }
        return lp.width;
    }

    @Property(value = "height", typeAdapters = {PxDimensionAdapter.class, DpDimensionAdapter.class, LayoutSizeDimensionAdapter.class})
    public int height(View view, Integer value, boolean set) {
        LayoutParams lp = view.getLayoutParams();
        if (set && value != null) {
            lp.height = value;
            view.setLayoutParams(lp);
        }
        return lp.height;
    }

    @Property(value = "margin left", typeAdapters = {PxDimensionAdapter.class, DpDimensionAdapter.class, LayoutSizeDimensionAdapter.class})
    public int marginLeft(View view, Integer value, boolean set) {
        if (!(view.getLayoutParams() instanceof MarginLayoutParams)) return 0;
        MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        if (set && value != null) {
            lp.leftMargin = value;
            view.setLayoutParams(lp);
        }
        return lp.leftMargin;
    }

    @Property(value = "margin top", typeAdapters = {PxDimensionAdapter.class, DpDimensionAdapter.class, LayoutSizeDimensionAdapter.class})
    public int marginTop(View view, Integer value, boolean set) {
        if (!(view.getLayoutParams() instanceof MarginLayoutParams)) return 0;
        MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        if (set && value != null) {
            lp.leftMargin = value;
            view.setLayoutParams(lp);
        }
        return lp.topMargin;
    }

    @Property(value = "margin right", typeAdapters = {PxDimensionAdapter.class, DpDimensionAdapter.class, LayoutSizeDimensionAdapter.class})
    public int marginRight(View view, Integer value, boolean set) {
        if (!(view.getLayoutParams() instanceof MarginLayoutParams)) return 0;
        MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        if (set && value != null) {
            lp.leftMargin = value;
            view.setLayoutParams(lp);
        }
        return lp.rightMargin;
    }

    @Property(value = "margin bottom", typeAdapters = {PxDimensionAdapter.class, DpDimensionAdapter.class, LayoutSizeDimensionAdapter.class})
    public int marginBottom(View view, Integer value, boolean set) {
        if (!(view.getLayoutParams() instanceof MarginLayoutParams)) return 0;
        MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        if (set && value != null) {
            lp.bottomMargin = value;
            view.setLayoutParams(lp);
        }
        return lp.bottomMargin;
    }

    @Property(value = "weight")
    public float weight(View view, Float value, boolean set) {
        if (!(view.getLayoutParams() instanceof LinearLayout.LayoutParams)) return 0;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
        if (set && value != null) {
            lp.weight = value;
            view.setLayoutParams(lp);
        }
        return lp.weight;
    }

    @Override
    protected boolean showGetterSetter(Object o, Method method) {
        if (method.getName().contains("Margin")) {
            return ((View) o).getLayoutParams() instanceof MarginLayoutParams;
        }
        if (method.getName().contains("weight")) {
            return ((View) o).getLayoutParams() instanceof LinearLayout.LayoutParams;
        }
        return super.showGetterSetter(o, method);
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return View.class.isAssignableFrom(clazz);
    }
}
