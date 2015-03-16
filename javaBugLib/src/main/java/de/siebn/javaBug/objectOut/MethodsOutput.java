package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by Sieben on 16.03.2015.
 */
public class MethodsOutput implements OutputCategory {
    private final JavaBug javaBug;

    public MethodsOutput(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    @Override
    public void add(XML ul, Object o) {
        ObjectBugPlugin.AllClassMembers allMembers = javaBug.getObjectBug().getAllMembers(o.getClass());
        for (Method m : allMembers.methods) {
            addMethodInformation(ul, o, m);
        }
    }

    @Override
    public String getType() {
        return "methods";
    }

    public void addMethodInformation(XML ul, Object o, Method m) {
        m.setAccessible(true);
        XML li = ul.add("li").setClass("object notOpenable");
        li.addClass(StringifierUtil.modifiersToString(m.getModifiers(), "mod", true));
        addModifiers(li, m.getModifiers());
        li.add("span").setClass("type").appendText(m.getReturnType().getSimpleName());
        li.appendText(" ").add("span").setClass("fieldName").appendText(m.getName());
        li.appendText("(");
        boolean first = true;
        for (Class c : m.getParameterTypes()) {
            if (!first) li.appendText(", ");
            li.add("span").setClass("type").appendText(c.getSimpleName());
            first = false;
        }
        li.appendText(")");
        if (m.getParameterTypes().length == 0) {
            li.setAttr("invoke", "/invoke/" + javaBug.getObjectBug().getObjectReference(o) + "/" + m.getName());
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
        return "Methods";
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
