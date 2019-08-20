package de.siebn.javaBug.objectOut;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import de.siebn.javaBug.BugElement.BugInvokable;
import de.siebn.javaBug.BugElement.BugExpandableEntry;
import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.BugElement.BugInlineList;
import de.siebn.javaBug.BugElement.BugInput;
import de.siebn.javaBug.BugElement.BugText;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.objectOut.ListItemBuilder.ParameterBuilder;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.plugins.ObjectBugPlugin.InvocationLinkBuilder;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

public abstract class AbstractOutputCategory implements OutputCategory {
    private final static Object[] empty = new Object[0];
    protected final JavaBug javaBug;
    protected final String type;
    protected final String name;
    protected int order;

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Property {
        String value();

        Class<?>[] typeAdapters() default {};
    }

    public AbstractOutputCategory(JavaBug javaBug, String type, String name, int order) {
        this.javaBug = javaBug;
        this.type = type;
        this.name = name;
        this.order = order;
    }

    @Override
    public String getId() {
        return getClass().getSimpleName();
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

    @Override
    public void add(BugGroup list, Object o) {
        for (Method m : AllClassMembers.getForClass(getClass()).methods) {
            Property getterSetter = m.getAnnotation(Property.class);
            if (getterSetter != null && showGetterSetter(o, m)) {
                addProperty(list, getterSetter, o, m);
            }
        }
    }

    @Override
    public void add(XML ul, Object o) {
        for (Method m : AllClassMembers.getForClass(getClass()).methods) {
            Property getterSetter = m.getAnnotation(Property.class);
            if (getterSetter != null && showGetterSetter(o, m)) {
                addProperty(ul, getterSetter, o, m);
            }
        }
    }

    protected boolean showGetterSetter(Object o, Method method) {
        return true;
    }

    @SuppressWarnings("unchecked")
    private void addProperty(BugGroup list, Property property, Object o, Method setter) {
        BugExpandableEntry json = new BugExpandableEntry();

        Object val = null;
        try {
            val = setter.invoke(this, o, null, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<TypeAdapter<Object>> typeAdapters = new ArrayList<>();
        if (property.typeAdapters().length != 0) {
            typeAdapters.addAll(TypeAdapters.getTypeAdapterClasses(property.typeAdapters()));
        } else {
            typeAdapters.add(null);
        }

        json.elements.add(new BugText(property.value()).setClazz("title").setClazz("title").setOnClick(BugText.ON_CLICK_EXPAND));

        for (TypeAdapter typeAdapter : typeAdapters) {
            BugInvokable invokable = new BugInvokable(BugInvokable.ACTION_REFRESH_ELEMENTS);
            BugInput input = new BugInput("p1", null);
            invokable.elements.add(input);
            InvocationLinkBuilder invocation = javaBug.getObjectBug().new InvocationLinkBuilder().setObject(this).setMethod(setter).setPredefined(0, o);
            if (typeAdapter != null) {
                invocation.setTypeAdapter(1, typeAdapter).setReturTypeAdapter(typeAdapter);
                input.text = typeAdapter.toString(val);
                invokable.elements.add(new BugText(typeAdapter.getUnit()));
            } else {
                input.text = TypeAdapters.toString(val);
            }
            invokable.url = invocation.setPredefined(2, true).build();
            input.setRefreshUrl(invocation.setPredefined(2, false).build());
            json.elements.add(BugText.NBSP);
            json.elements.add(invokable);
        }

        list.elements.add(json);
    }

    public void addMethodInformation(BugGroup list, Object o, Method m, Object[] predifined, Object[] preset) {
        if (predifined == null) predifined = empty;
        if (preset == null) preset = empty;
        boolean canInvoke = true;
        Class<?>[] parameterTypes = m.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class c = parameterTypes[i];
            if (!TypeAdapters.getTypeAdapter(c).canParse(c) && (predifined.length <= i || predifined[i] == null))
                canInvoke = false;
        }
        BugExpandableEntry json = new BugExpandableEntry();
        json.elements.add(BugText.getForModifier(m.getModifiers()));
        json.elements.add(BugText.getForClass(m.getReturnType()));
        json.elements.add(new BugText(m.getName()).setClazz("name").setOnClick(BugText.ON_CLICK_EXPAND));

        BugInvokable invokable = new BugInvokable(BugInvokable.ACTION_EXPAND_RESULT);
        for (int i = 0; i < parameterTypes.length; i++) {
            invokable.elements.add(BugText.getForClass(parameterTypes[i]));
            BugInput parameter = new BugInput("p" + i, preset.length > i ? TypeAdapters.toString(preset[i]) : null);
            invokable.elements.add(parameter);
        }
        if (canInvoke) invokable.url = javaBug.getObjectBug().getInvokationLink(ObjectBugPlugin.RETURN_TYPE_JSON, o, m);
        invokable.addBraces();
        invokable.elements.add(BugText.INVOKER);
        json.elements.add(invokable);

        list.elements.add(json);
    }

    public void addFieldInformation(BugGroup list, Object o, Field f) {
        Object val = null;
        try {
            val = f.get(o);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        BugExpandableEntry json = new BugExpandableEntry();

        json.elements.add(BugText.getForModifier(f.getModifiers()));
        json.elements.add(BugText.getForClass(f.getType()));
        json.elements.add(new BugText(f.getName()).setClazz("name").setOnClick(BugText.ON_CLICK_EXPAND));

        json.elements.add(BugText.VALUE_SEPARATOR);
        json.elements.add(BugText.getForValue(val));
        list.elements.add(json);
    }

    public void addPojo(BugGroup list, Object o, String field) {
        AllClassMembers.POJO pojo = AllClassMembers.getForClass(o.getClass()).pojos.get(field);
        if (pojo == null) return;

        boolean setAble = pojo.setter != null && TypeAdapters.canParse(pojo.setter.getParameterTypes()[0]);
        if (!setAble && pojo.getter == null) return;

        ListItemBuilder builder = new ListItemBuilder();

        Object val = null;
        if (pojo.getter != null) {
            try {
                val = pojo.getter.invoke(o);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            builder.setExpandObject(javaBug.getObjectBug(), val, pojo.getter.getReturnType());
        }

        BugExpandableEntry json = new BugExpandableEntry();
        json.elements.add(new BugText(field).setClazz("title").setOnClick(BugText.ON_CLICK_EXPAND));
        json.elements.add(BugText.VALUE_SEPARATOR);
        json.elements.add(BugText.getForValue(val));
        list.elements.add(json);
    }


    private void addProperty(XML ul, Property property, Object o, Method setter) {
        ListItemBuilder builder = new ListItemBuilder();
        builder.setName(property.value());
        Object val = null;
        try {
            val = setter.invoke(this, o, null, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<TypeAdapter<Object>> typeAdapters = new ArrayList<>();
        if (property.typeAdapters().length != 0) {
            typeAdapters.addAll(TypeAdapters.getTypeAdapterClasses(property.typeAdapters()));
        } else {
            typeAdapters.add(null);
        }

        for (TypeAdapter<?> typeAdapter : typeAdapters) {
            ParameterBuilder value = builder.addValue();
            value.setValue(val);
            InvocationLinkBuilder invocation = javaBug.getObjectBug().new InvocationLinkBuilder().setObject(this).setMethod(setter).setPredefined(0, o);
            if (typeAdapter != null) {
                invocation.setTypeAdapter(1, typeAdapter).setReturTypeAdapter(typeAdapter);
                value.setUnit(typeAdapter.getUnit());
            }
            value.setEditLink(invocation.setPredefined(2, true).build());
            value.setUpdateLink(invocation.setPredefined(2, false).build());
            value.setNullable(!setter.getReturnType().isPrimitive());
            value.setParameterNum(1);
            value.setTypeAdapter(typeAdapter);
        }

        builder.build(ul);
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
        ListItemBuilder builder = new ListItemBuilder();
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
        if (canInvoke) builder.setInvokeLink(javaBug.getObjectBug().getInvokationLink(ObjectBugPlugin.RETURN_TYPE_XML, o, m));
        builder.build(ul);
    }

    public void addPojo(XML ul, Object o, String field) {
        AllClassMembers.POJO pojo = AllClassMembers.getForClass(o.getClass()).pojos.get(field);
        if (pojo == null) return;

        boolean setAble = pojo.setter != null && TypeAdapters.canParse(pojo.setter.getParameterTypes()[0]);
        if (!setAble && pojo.getter == null) return;

        ListItemBuilder builder = new ListItemBuilder();

        Object val = null;
        if (pojo.getter != null) {
            try {
                val = pojo.getter.invoke(o);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            builder.setExpandObject(javaBug.getObjectBug(), val, pojo.getter.getReturnType());
        }

        builder.setName(field);
        ParameterBuilder value = builder.addValue();
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
        ListItemBuilder builder = new ListItemBuilder();
        builder.setName(f.getName());
        builder.setType(f.getType());
        builder.setModifiers(f.getModifiers());
        ParameterBuilder value = builder.addValue();
        TypeAdapters.TypeAdapter<Object> adapter = TypeAdapters.getTypeAdapter(f.getType());
        value.setValue(val);
        if (adapter != null && !Modifier.isFinal(f.getModifiers()) && adapter.canParse(f.getType())) {
            value.setEditLink(javaBug.getObjectBug().getObjectEditLink(o, f));
            value.setNullable(!f.getType().isPrimitive());
        }
        builder.setExpandObject(javaBug.getObjectBug(), val, f.getType());
        builder.build(ul);
    }

    protected void addModifiers(XML tag, int modifiers) {
        if (modifiers != 0) {
            tag.add("span").setClass("modifier").appendText(StringifierUtil.modifiersToString(modifiers, null, false));
            tag.appendText(" ");
        }
    }
}
