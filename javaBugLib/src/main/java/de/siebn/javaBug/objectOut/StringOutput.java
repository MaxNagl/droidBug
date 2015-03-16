package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Field;

/**
 * Created by Sieben on 16.03.2015.
 */
public class StringOutput implements OutputCategory {
    private final JavaBug javaBug;

    public StringOutput(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    @Override
    public void add(XML ul, Object o) {
        TypeAdapters.TypeAdapter adapter = TypeAdapters.getTypeAdapter(o.getClass());
        XML li = ul.add("li").setClass("object");
        li.setAttr("expand", javaBug.getObjectBug().getObjectDetailsLink(o));
        li.add("span").setClass("type").appendText(o.getClass().getSimpleName());
        li.appendText(" ").add("span").setClass("value").appendText(TypeAdapters.toString(o));
    }

    @Override
    public String getType() {
        return "string";
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return true;
    }

    @Override
    public boolean opened() {
        return false;
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
