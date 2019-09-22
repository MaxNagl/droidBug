package de.siebn.javaBug.android;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;

import java.lang.reflect.Field;
import java.util.ArrayList;

import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.objectOut.BugSimpleOutputCategory;
import de.siebn.javaBug.util.BugGenericUtils;

public class LayoutParameterOutput extends BugSimpleOutputCategory<View> {
    private final ArrayList<LayoutParamProperty> properties = new ArrayList<>();

    private static class LayoutParamProperty extends AbstractProperty<View, Object> {
        private final Field field;

        public LayoutParamProperty(Class clazz, String fieldName) {
            super(null, null, true);
            field = BugGenericUtils.getFieldOrThrow(clazz, fieldName);
            super.name = field.getName();
            super.clazz = field.getType();
        }

        @Override
        public Object getValue(View view) {
            LayoutParams lp = view.getLayoutParams();
            return BugGenericUtils.getOrNull(lp, field);
        }

        @Override
        public void setValue(View view, Object value) {
            LayoutParams lp = view.getLayoutParams();
            BugGenericUtils.set(lp, field, value);
            view.setLayoutParams(lp);
        }
    }

    public LayoutParameterOutput(JavaBugCore javaBug) {
        super(javaBug, "layoutparams", "Layout Params", 100);
        properties.add(new LayoutParamProperty(ViewGroup.LayoutParams.class, "width"));
        properties.add(new LayoutParamProperty(ViewGroup.LayoutParams.class, "height"));
        properties.add(new LayoutParamProperty(MarginLayoutParams.class, "bottomMargin"));
        properties.add(new LayoutParamProperty(MarginLayoutParams.class, "leftMargin"));
        properties.add(new LayoutParamProperty(MarginLayoutParams.class, "rightMargin"));
        properties.add(new LayoutParamProperty(MarginLayoutParams.class, "topMargin"));
        properties.add(new LayoutParamProperty(LinearLayout.LayoutParams.class, "gravity"));
        properties.add(new LayoutParamProperty(LinearLayout.LayoutParams.class, "weight"));
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return View.class.isAssignableFrom(clazz);
    }

    @Override
    public String getName(Object o) {
        String name = ((View) o).getLayoutParams().getClass().getName();
        if (name.lastIndexOf(".") > 0) name = name.substring(name.lastIndexOf(".") + 1);
        return name.replace('$', '.');
    }

    @Override
    protected void addElements(BugGroup parent, View o) {
        LayoutParams lp = o.getLayoutParams();
        for (LayoutParamProperty property : properties) {
            if (property.field.getDeclaringClass().isAssignableFrom(lp.getClass())) {
                addProperty(parent, o, property);
            }
        }
    }
}
