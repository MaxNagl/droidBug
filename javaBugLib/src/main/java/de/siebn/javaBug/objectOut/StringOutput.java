package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by Sieben on 16.03.2015.
 */
public class StringOutput extends AbstractOutputCategory {

    public StringOutput(JavaBug javaBug) {
        super(javaBug, "string", null, 0);
    }

    @Override
    public void add(XML ul, Object o) {
        XML li = ul.add("li").setClass("object");
        li.setAttr("expand", javaBug.getObjectBug().getObjectDetailsLink(o));
        li.add("span").setClass("type").appendText(o.getClass().getSimpleName());
        li.appendText(" ").add("span").setClass("value").appendText(TypeAdapters.toString(o));
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return true;
    }
}
