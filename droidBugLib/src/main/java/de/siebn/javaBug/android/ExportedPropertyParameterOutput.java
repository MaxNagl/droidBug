package de.siebn.javaBug.android;

import android.view.View;
import android.view.ViewDebug.ExportedProperty;

import java.util.*;

import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.BugFormat;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.objectOut.BugSimpleOutputCategory;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.BugProperty;

public class ExportedPropertyParameterOutput extends BugSimpleOutputCategory<Object> {
    private final HashMap<BugProperty, SimpleProperty> properties = new HashMap<>();

    public ExportedPropertyParameterOutput(JavaBugCore javaBug) {
        super(javaBug, "exportedProperty", "ExportedProperties", 100);
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return View.class.isAssignableFrom(clazz);
    }

    @Override
    protected void addElements(BugGroup parent, Object o) {
        addElements(parent, o, "");
    }

    private class Collected {
        private Object object;
        private SimpleProperty propery;
        private String category;
    }

    private void addElements(BugGroup parent, Object o, String prefix) {
        ArrayList<Collected> collection = new ArrayList<>();
        collectElements(o, prefix, collection);
        Collections.sort(collection, new Comparator<Collected>() {
            @Override
            public int compare(Collected o1, Collected o2) {
                return o1.category.compareTo(o2.category);
            }
        });
        String category = "";
        BugGroup group = parent;
        for (Collected collected : collection) {
            if (!category.equals(collected.category)) {
                category = collected.category;
                BugEntry entry = new BugEntry();
                entry.add(new BugText(category).format(BugFormat.colorSecondaryLight));
                entry.setExpand(group = new BugList());
                entry.setAutoExpand(true);
                parent.add(entry);
            }
            addProperty(group, collected.object, collected.propery);
        }
    }

    private void collectElements(Object o, String prefix, ArrayList<Collected> collection) {
        if (o == null) return;
        for (BugProperty property : AllClassMembers.getForClass(o.getClass()).properties) {
            ExportedProperty exportedProperty = property.getAnnotation(ExportedProperty.class);
            if (exportedProperty == null) continue;
            if (exportedProperty.deepExport()) {
                collectElements(property.getValue(o), prefix + exportedProperty.prefix(), collection);
            } else {
                SimpleProperty simple = properties.get(property);
                if (simple == null) {
                    properties.put(property, simple = new ReflectionProperty(property).setName(exportedProperty.category() + " " + prefix + exportedProperty.prefix() + property.getName()));
                }
                Collected collected = new Collected();
                collected.object = o;
                collected.propery = simple;
                collected.category = exportedProperty.category();
                collection.add(collected);
            }
        }
    }
}
