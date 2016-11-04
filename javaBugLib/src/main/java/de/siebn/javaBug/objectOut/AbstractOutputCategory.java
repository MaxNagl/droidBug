package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Sieben on 16.03.2015.
 */
public abstract class AbstractOutputCategory implements OutputCategory {
    private final static Object[] empty = new Object[0];
    protected final JavaBug javaBug;
    protected final String type;
    protected final String name;
    protected int order;

    public AbstractOutputCategory(JavaBug javaBug, String type, String name, int order) {
        this.javaBug = javaBug;
        this.type = type;
        this.name = name;
        this.order = order;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getName(Object o) {
        return name;
    }

    @Override
    public boolean opened(List<OutputCategory> others, boolean alreadyOpened) {
        return !alreadyOpened;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void addMethodInformation(XML ul, Object o, Method m, Object[] predifined, Object[] preset) {
        if (predifined == null) predifined = empty;
        if (preset == null) preset = empty;
        XML li = ul.add("li").setClass("object notOpenable");
        li.addClass(StringifierUtil.modifiersToString(m.getModifiers(), "mod", true));
        addModifiers(li, m.getModifiers());
        li.add("span").setClass("type").appendText(m.getReturnType().getSimpleName());
        li.appendText(" ").add("span").setClass("fieldName").appendText(m.getName());
        li.appendText("(");
        boolean first = true;
        boolean canInvoke = true;
        Class<?>[] parameterTypes = m.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class c = parameterTypes[i];
            if (!TypeAdapters.getTypeAdapter(c).canParse(c) && (predifined.length <= i || predifined[i] == null))
                canInvoke = false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (predifined.length > i && predifined[i] != null) {
                Class c = parameterTypes[i];
                li.add("span").setClass("type predefined").setAttr("value", javaBug.getObjectBug().getObjectReference(predifined[i])).setAttr("predifined", "o" + i).appendText(c.getSimpleName());
            } else {
                Class c = parameterTypes[i];
                if (!first) li.appendText(", ");
                li.add("span").setClass("type").appendText(c.getSimpleName());
                if (canInvoke) {
                    li.appendText(" ");
                    XML p = li.add("span").setClass("parameter").setAttr("parameter", "p" + i);
                    if (preset.length > i && preset[i] != null) {
                        p.appendText(TypeAdapters.getTypeAdapter(preset[i].getClass()).toString(preset[i]));
                    }
                }
                first = false;
            }
        }
        li.appendText(")");
        if (canInvoke) {
            li.setAttr("invoke", javaBug.getObjectBug().getInvokationLink(true, o, m));
        }
    }

    public void addPojo(XML ul, Object o, String field) {
        AllClassMembers.POJO pojo = AllClassMembers.getForClass(o.getClass()).pojos.get(field);
        if (pojo == null) return;
        boolean setAble = pojo.setter != null && TypeAdapters.canParse(pojo.setter.getParameterTypes()[0]);
        if (!setAble && pojo.getter == null) return;
        XML li = ul.add("li").setClass("object notOpenable");
        li.appendText(" ").add("span").setClass("fieldName").appendText(field);
        li.appendText(": ");
        Object val = null;
        if (pojo.getter != null) {
            try {
                val = pojo.getter.invoke(o);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        XML p = li.add("span").setClass("parameter").appendText(String.valueOf(val));
        if (setAble) {
            p.setAttr("editurl", javaBug.getObjectBug().getPojoLink(o, field));
            if (!pojo.setter.getParameterTypes()[0].isPrimitive())
                p.setAttr("editNullify", "true");
        }
    }

    public void addFieldInformation(XML ul, Object o, Field f) {
        XML li = ul.add("li").setClass("object");
        li.addClass(StringifierUtil.modifiersToString(f.getModifiers(), "mod", true));
        addModifiers(li, f.getModifiers());
        li.add("span").setClass("type").appendText(f.getType().getSimpleName());
        li.add("span").appendText(" ").setClass("fieldName").appendText(f.getName());
        li.add("span").setClass("equals").appendText(" = ");
        try {
            javaBug.getObjectBug().addObjectInfo(li, f.get(o), o, f);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    protected void addModifiers(XML tag, int modifiers) {
        if (modifiers != 0) {
            tag.add("span").setClass("modifier").appendText(StringifierUtil.modifiersToString(modifiers, null, false));
            tag.appendText(" ");
        }
    }
}
