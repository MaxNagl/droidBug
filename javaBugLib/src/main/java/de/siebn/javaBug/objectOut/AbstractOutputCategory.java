package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.objectOut.PropertyBuilder.ParameterBuilder;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
        boolean canInvoke = true;
        Class<?>[] parameterTypes = m.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class c = parameterTypes[i];
            if (!TypeAdapters.getTypeAdapter(c).canParse(c) && (predifined.length <= i || predifined[i] == null))
                canInvoke = false;
        }
        PropertyBuilder builder = new PropertyBuilder(javaBug.getObjectBug());
        builder.setModifiers(m.getModifiers());
        builder.setType(m.getReturnType());
        builder.setName(m.getName());
        for (int i = 0; i < parameterTypes.length; i++) {
            ParameterBuilder param = builder.addParameter();
            param.setType(parameterTypes[i]);
            if (canInvoke) {
                param.setParameterNum(i);
                param.setValue(preset.length > i ? preset[i] : null);
            }
        }
        if (canInvoke) builder.setInvokeLink(javaBug.getObjectBug().getInvokationLink(true, o, m));
        builder.build(ul);
    }

    public void addPojo(XML ul, Object o, String field) {
        AllClassMembers.POJO pojo = AllClassMembers.getForClass(o.getClass()).pojos.get(field);
        if (pojo == null) return;

        boolean setAble = pojo.setter != null && TypeAdapters.canParse(pojo.setter.getParameterTypes()[0]);
        if (!setAble && pojo.getter == null) return;

        PropertyBuilder builder = new PropertyBuilder(javaBug.getObjectBug());

        Object val = null;
        if (pojo.getter != null) {
            try {
                val = pojo.getter.invoke(o);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            builder.setExpandObject(val, pojo.getter.getReturnType());
        }

        builder.setName(field);
        ParameterBuilder value = builder.createValue();
        value.setValue(val);
        if (setAble) {
            value.setEditLink(javaBug.getObjectBug().getPojoLink(o, field));
            value.setNullable(!pojo.setter.getParameterTypes()[0].isPrimitive());
        }
        builder.build(ul);
    }

    public void addFieldInformation(XML ul, Object o, Field f) {
        Object val = null;
        try {
            val = f.get(o);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        PropertyBuilder builder = new PropertyBuilder(javaBug.getObjectBug());
        builder.setName(f.getName());
        builder.setType(f.getType());
        builder.setModifiers(f.getModifiers());
        ParameterBuilder value = builder.createValue();
        TypeAdapters.TypeAdapter<Object> adapter = TypeAdapters.getTypeAdapter(f.getType());
        value.setValue(val);
        if (adapter != null && !Modifier.isFinal(f.getModifiers()) && adapter.canParse(f.getType())) {
            value.setEditLink(javaBug.getObjectBug().getObjectEditLink(o, f));
            value.setNullable(!f.getType().isPrimitive());
        }
        builder.setExpandObject(val, f.getType());
        builder.build(ul);
    }

    protected void addModifiers(XML tag, int modifiers) {
        if (modifiers != 0) {
            tag.add("span").setClass("modifier").appendText(StringifierUtil.modifiersToString(modifiers, null, false));
            tag.appendText(" ");
        }
    }
}
