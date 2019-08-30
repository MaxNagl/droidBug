package de.siebn.javaBug.objectOut;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.plugins.ObjectBugPlugin.InvokationLinkBuilder;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeSelectionAdapter;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.StringifierUtil;

import static de.siebn.javaBug.BugFormat.field;
import static de.siebn.javaBug.BugFormat.method;

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

    @Retention(RetentionPolicy.RUNTIME)
    public @interface OutputMethod {
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
    public BugElement get(Object o) {
        BugList list = new BugList();
        for (Method m : AllClassMembers.getForClass(getClass()).methods) {
            Property getterSetter = m.getAnnotation(Property.class);
            if (getterSetter != null && showGetterSetter(o, m)) {
                list.add(getProperty(getterSetter, o, m));
            }
            OutputMethod outputMethod = m.getAnnotation(OutputMethod.class);
            if (outputMethod != null && showOutputMethod(o, m)) {
                list.add(getMethodInformation(this, m, new Object[]{o}, null));
            }
        }
        return list;
    }

    protected boolean showOutputMethod(Object o, Method method) {
        return true;
    }

    protected boolean showGetterSetter(Object o, Method method) {
        return true;
    }

    @SuppressWarnings("unchecked")
    private BugElement getProperty(Property property, Object o, Method setter) {
        BugEntry json = new BugEntry();

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

        json.add(new BugText(property.value()).format(field).setOnClick(BugText.ON_CLICK_EXPAND));
        json.add(BugText.VALUE_SEPARATOR);

        for (TypeAdapter typeAdapter : typeAdapters) {
            BugInvokable invokable = new BugInvokable(BugInvokable.ACTION_REFRESH_ELEMENTS);
            String text = typeAdapter == null ? TypeAdapters.toString(val) : typeAdapter.toString(val);
            BugInputElement input;
            if (typeAdapter instanceof TypeSelectionAdapter) {
                BugInputList inputList = new BugInputList("p1", null);
                inputList.addMap(((TypeSelectionAdapter) typeAdapter).getValues());
                inputList.text = text;
                input = inputList;
            } else {
                BugInputText bugInput = new BugInputText("p1", null);
                bugInput.text = text;
                input = bugInput;
            }
            invokable.add(input);
            InvokationLinkBuilder invocation = new InvokationLinkBuilder(setter, this).setPredefined(0, o);
            if (typeAdapter != null) {
                invocation.setTypeAdapter(1, typeAdapter).setReturTypeAdapter(typeAdapter);
                invokable.add(new BugText(typeAdapter.getUnit()));
            }
            invokable.url = invocation.setPredefined(2, true).build();
            input.setRefreshUrl(invocation.setPredefined(2, false).build());
            json.add(BugText.NBSP);
            json.add(invokable);
        }

        return json;
    }

    public BugElement getMethodInformation(Object o, Method m, Object[] predefined, Object[] preset) {
        if (predefined == null) predefined = empty;
        if (preset == null) preset = empty;
        boolean canInvoke = true;
        Class<?>[] parameterTypes = m.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class c = parameterTypes[i];
            if (!TypeAdapters.getTypeAdapter(c).canParse(c) && (predefined.length <= i || predefined[i] == null))
                canInvoke = false;
        }
        BugEntry json = new BugEntry();
        json.addClazz(StringifierUtil.modifiersToString(m.getModifiers(), "mod", true));
        json.add(BugText.getForModifier(m.getModifiers())).addSpace();
        json.add(BugText.getForClass(m.getReturnType())).addSpace();
        json.add(new BugText(m.getName()).format(method).setOnClick(BugText.ON_CLICK_EXPAND));

        BugInvokable invokable = new BugInvokable(BugInvokable.ACTION_EXPAND_RESULT);
        boolean firstParameter = true;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (predefined.length > i && predefined[i] != null) continue;
            if (firstParameter) invokable.add(new BugText(", "));
            invokable.add(BugText.getForClass(parameterTypes[i])).addSpace();
            invokable.add(new BugInputText("p" + i, preset.length > i ? TypeAdapters.toString(preset[i]) : null));
            firstParameter = false;
        }
        if (canInvoke) invokable.url = new InvokationLinkBuilder(m, o).setReturnType(ObjectBugPlugin.RETURN_TYPE_JSON).setPredefined(predefined).build();
        invokable.addBraces();
        invokable.add(BugText.INVOKER);
        json.add(invokable);
        return json;
    }

    public BugElement getFieldInformation(Object o, Field f) {
        Object val = null;
        try {
            val = f.get(o);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        BugEntry json = new BugEntry();
        json.setExpandInclude(javaBug.getObjectBug().getObjectDetailsLink(val));
        json.addClazz(StringifierUtil.modifiersToString(f.getModifiers(), "mod", true));
        json.add(BugText.getForModifier(f.getModifiers())).addSpace();
        json.add(BugText.getForClass(f.getType())).addSpace();
        json.add(new BugText(f.getName()).format(field).setOnClick(BugText.ON_CLICK_EXPAND));

        json.add(BugText.VALUE_SEPARATOR);
        TypeAdapters.TypeAdapter<Object> adapter = TypeAdapters.getTypeAdapter(f.getType());
        if (adapter != null) {
            BugInvokable invokable = new BugInvokable(BugInvokable.ACTION_SET_VALUE);
            BugInputText inputText = new BugInputText("value", adapter.toString(val));
            inputText.refreshUrl = javaBug.getObjectBug().getObjectGetLink(o, f);
            if (!Modifier.isFinal(f.getModifiers()) && adapter.canParse(f.getType())) {
                invokable.url = javaBug.getObjectBug().getObjectEditLink(o, f);
            } else {
                inputText.enabled = false;
            }
            inputText.nullable = !f.getType().isPrimitive();
            invokable.add(inputText);
            json.add(invokable);
        }
        return json;
    }

    public BugElement getPojo(Object o, String field) {
        AllClassMembers.POJO pojo = AllClassMembers.getForClass(o.getClass()).pojos.get(field);
        if (pojo == null) return null;

        boolean setAble = pojo.setter != null && TypeAdapters.canParse(pojo.setter.getParameterTypes()[0]);
        if (!setAble && pojo.getter == null) return null;

        Object val = null;
        if (pojo.getter != null) {
            try {
                val = pojo.getter.invoke(o);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        BugEntry json = new BugEntry();
        json.add(new BugText(field).setOnClick(BugText.ON_CLICK_EXPAND).format(BugFormat.field));
        json.add(BugText.VALUE_SEPARATOR);
        json.add(BugText.getForValue(val));
        return json;
    }
}
