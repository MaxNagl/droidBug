package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Field;

/**
 * Created by Sieben on 16.03.2015.
 */
public class FieldsOutput implements OutputCategory {
    private final JavaBug javaBug;

    public FieldsOutput(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    @Override
    public void add(XML ul, Object o) {
        ObjectBugPlugin.AllClassMembers allMembers = javaBug.getObjectBug().getAllMembers(o.getClass());
        for (Field f : allMembers.fields) {
            addFieldInformation(ul, o, f);
        }
    }

    @Override
    public String getType() {
        return "fields";
    }

    public void addFieldInformation(XML ul, Object o, Field f) {
        f.setAccessible(true);
        XML li = ul.add("li").setClass("object");
        li.addClass(StringifierUtil.modifiersToString(f.getModifiers(), "mod", true));
        addModifiers(li, f.getModifiers());
        li.add("span").setClass("type").appendText(f.getType().getSimpleName());
        li.add("span").appendText(" ").setClass("fieldName").appendText(f.getName());
        li.add("span").setClass("equals").appendText(" = ");
        try {
            javaBug.getObjectBug().addObjectInfo(li, f.get(o), javaBug.getObjectBug().getObjectReference(o), f);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void addModifiers(XML tag, int modifiers) {
        if (modifiers != 0) {
            tag.add("span").setClass("modifier").appendText(StringifierUtil.modifiersToString(modifiers, null, false));
            tag.appendText(" ");
        }
    }

    @Override
    public String getName() {
        return "Fields";
    }

    @Override
    public boolean opened() {
        return true;
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return true;
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
