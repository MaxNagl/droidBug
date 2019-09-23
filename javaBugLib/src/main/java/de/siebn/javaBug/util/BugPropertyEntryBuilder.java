package de.siebn.javaBug.util;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.BugFormat;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.plugins.ObjectBugPlugin.InvocationLinkBuilder;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;

public class BugPropertyEntryBuilder {
    private String name;
    private Object value;
    private Class clazz;
    private int modifier;
    private int paramIndex;
    private InvocationLinkBuilder setter;
    private InvocationLinkBuilder getter;
    private List<TypeAdapter<Object>> typeAdapters;

    public BugPropertyEntryBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public BugPropertyEntryBuilder setValue(Object value) {
        this.value = value;
        return this;
    }

    public BugPropertyEntryBuilder setParamIndex(int paramIndex) {
        this.paramIndex = paramIndex;
        return this;
    }

    public BugPropertyEntryBuilder setSetter(InvocationLinkBuilder setter) {
        this.setter = setter;
        return this;
    }

    public BugPropertyEntryBuilder setGetter(InvocationLinkBuilder getter) {
        this.getter = getter;
        return this;
    }

    public BugPropertyEntryBuilder setClazz(Class clazz) {
        this.clazz = clazz;
        return this;
    }

    public BugPropertyEntryBuilder setModifier(int modifier) {
        this.modifier = modifier;
        return this;
    }

    public BugPropertyEntryBuilder setTypeAdapter(TypeAdapter<Object> typeAdapter) {
        typeAdapters = new ArrayList<>();
        typeAdapters.add(typeAdapter);
        return this;
    }

    public BugPropertyEntryBuilder setTypeAdapters(List<TypeAdapter<Object>> typeAdapters) {
        this.typeAdapters = typeAdapters;
        return this;
    }

    @SuppressWarnings("unchecked")
    public BugElement build() {
        BugEntry entry = new BugEntry();

        if (modifier != 0) entry.add(BugText.getForModifier(modifier)).addSpace();
        entry.add(BugText.getForClass(clazz)).addSpace();
        entry.add(new BugText(name).format(BugFormat.field).setOnClick(BugText.ON_CLICK_EXPAND));
        entry.add(BugText.VALUE_SEPARATOR);

        if (typeAdapters == null || typeAdapters.size() == 0) {
            typeAdapters = new ArrayList<>();
            typeAdapters.add(null);
        }

        for (TypeAdapter typeAdapter : typeAdapters) {
            boolean defaultTypeAdapter = typeAdapter == null;
            if (defaultTypeAdapter) typeAdapter = TypeAdapters.getTypeAdapter(clazz);
            BugInvokable invokable = new BugInvokable(BugInvokable.ACTION_REFRESH_ELEMENTS);
            BugInputElement input = BugInputElementBuilder.build(value, clazz, paramIndex, typeAdapter);
            invokable.add(input);
            if (setter != null && typeAdapter.canParse(clazz)) {
                if (!defaultTypeAdapter) setter.setTypeAdapter(paramIndex, typeAdapter);
                invokable.url = setter.build();
            } else {
                input.enabled = false;
            }
            if (getter != null) {
                if (!defaultTypeAdapter) getter.setReturnTypeAdapter(typeAdapter);
                input.setRefreshUrl(getter.build());
            }
            invokable.add(new BugText(typeAdapter.getUnit()));
            entry.add(BugText.NBSP);
            entry.add(invokable);
            entry.setExpandInclude(ObjectBugPlugin.getObjectDetailsLink(value));
        }

        return entry;
    }

    public static BugPropertyEntryBuilder getForGetterSetter(String name, Object o, Method getterSetter, Object target, List<TypeAdapter<Object>> typeAdapters) {
        return new BugPropertyEntryBuilder()
                .setName(name)
                .setValue(BugReflectionUtils.invokeOrNull(o, getterSetter, target, null, false))
                .setClazz(getterSetter.getReturnType())
                .setParamIndex(1)
                .setSetter(new InvocationLinkBuilder(getterSetter, o).setPredefined(0, target).setPredefined(2, true))
                .setGetter(new InvocationLinkBuilder(getterSetter, o).setPredefined(0, target).setPredefined(2, false))
                .setTypeAdapters(typeAdapters);
    }

    public static BugPropertyEntryBuilder getForField(Object o, Field f) {
        return new BugPropertyEntryBuilder()
                .setName(f.getName())
                .setParamIndex(2)
                .setClazz(f.getType())
                .setModifier(f.getModifiers())
                .setValue(BugReflectionUtils.getOrNull(o, f))
                .setSetter(InvocationLinkBuilder.getSetter(o, f))
                .setGetter(InvocationLinkBuilder.getGetter(o, f));
    }

    public static BugPropertyEntryBuilder getForPojo(Object o, String fieldName) {
        AllClassMembers.POJO pojo = AllClassMembers.getForClass(o.getClass()).pojos.get(fieldName);
        if (pojo == null || (pojo.getter == null && pojo.setter == null)) return null;
        return new BugPropertyEntryBuilder()
                .setName(fieldName)
                .setClazz(pojo.getter == null ? pojo.setter.getParameterTypes()[0] : pojo.getter.getReturnType())
                .setValue(pojo.getter == null ? null : BugReflectionUtils.invokeOrNull(o, pojo.getter))
                .setSetter(pojo.setter == null ? null : new InvocationLinkBuilder(pojo.setter, o))
                .setGetter(pojo.getter == null ? null : new InvocationLinkBuilder(pojo.getter, o));
    }
}
