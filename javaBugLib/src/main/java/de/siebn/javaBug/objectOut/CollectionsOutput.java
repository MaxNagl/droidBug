package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.BugFormat;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.util.UnicodeCharacters;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Sieben on 16.03.2015.
 */
public class CollectionsOutput extends AbstractOutputCategory {

    public CollectionsOutput(JavaBugCore javaBug) {
        super(javaBug, "array", "Array", 500);
    }

    @Override
    @SuppressWarnings("StringConcatenationInLoop")
    public BugElement get(Object o) {
        BugList list = new BugList();

        boolean isArray = o.getClass().isArray();
        int len = 0, indexLen = 0;
        Iterator keyIt = null, valIt = null;
        if (isArray) {
            len = Array.getLength(o);
            indexLen = String.valueOf(len - 1).length();
        }
        if (o instanceof Collection) {
            len = ((Collection) o).size();
            valIt = ((Collection) o).iterator();
        }
        if (o instanceof Map) {
            len = ((Map) o).size();
            keyIt = ((Map) o).keySet().iterator();
            valIt = ((Map) o).values().iterator();
        }
        for (int i = 0; i < len; i++) {
            BugEntry entry = new BugEntry();
            Object key = null;
            if (keyIt != null) {
                key = keyIt.next();
                entry.add(BugText.getForValue(key).setOnClick(BugElement.ON_CLICK_EXPAND));
            } else {
                String index = String.valueOf(i);
                while (index.length() < indexLen) index = UnicodeCharacters.NBSP + index;
                entry.add(new BugText(index).format(BugFormat.field).setOnClick(BugElement.ON_CLICK_EXPAND));
            }
            entry.addText(" -> ");
            Object val = valIt != null ? valIt.next() : Array.get(o, i);
            entry.add(BugText.getForValue(val).setOnClick(BugElement.ON_CLICK_EXPAND));
            if (key != null) {
                BugList expand = new BugList();
                expand.add(javaBug.getObjectBug().getObjectElement(null, "Key", key));
                expand.add(javaBug.getObjectBug().getObjectElement(null, "Value", val));
                entry.setExpand(expand);
            } else {
                entry.setExpandInclude(javaBug.getObjectBug().getObjectDetailsLink(val));
            }
            list.add(entry);
        }
        return list;
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return clazz.isArray() || Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
    }

    @Override
    public String getName(Object o) {
        boolean isArray = o.getClass().isArray();
        int len = isArray ? Array.getLength(o) : o instanceof Map ? ((Map) o).size() : ((Collection) o).size();
        String name = isArray ? o.getClass().getComponentType().getSimpleName() : o.getClass().getSimpleName();
        return name + "[" + len + "]";
    }
}
