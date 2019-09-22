package de.siebn.javaBug.util;

import java.util.ArrayList;
import java.util.List;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.BugFormat;
import de.siebn.javaBug.plugins.ObjectBugPlugin.InvocationLinkBuilder;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeSelectionAdapter;

public class BugPropertyEntryBuilder {
    private String name;
    private Object value;
    private Class clazz;
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

        entry.add(new BugText(name).format(BugFormat.field).setOnClick(BugText.ON_CLICK_EXPAND));
        entry.add(BugText.VALUE_SEPARATOR);

        if (typeAdapters == null || typeAdapters.size() == 0) {
            typeAdapters = new ArrayList<>();
            typeAdapters.add(TypeAdapters.getTypeAdapter(clazz));
        }

        for (TypeAdapter typeAdapter : typeAdapters) {
            BugInvokable invokable = new BugInvokable(BugInvokable.ACTION_REFRESH_ELEMENTS);
            String text = typeAdapter.toString(value);
            BugInputElement input;
            if (typeAdapter instanceof TypeSelectionAdapter) {
                BugInputList inputList = new BugInputList("p" + paramIndex, null);
                inputList.addMap(((TypeSelectionAdapter) typeAdapter).getValues(clazz));
                inputList.text = text;
                input = inputList;
            } else {
                BugInputText bugInput = new BugInputText("p" + paramIndex, null);
                bugInput.text = text;
                input = bugInput;
            }
            invokable.add(input);
            if (setter != null && typeAdapter.canParse(clazz)) {
                setter.setTypeAdapter(paramIndex, typeAdapter);
                invokable.url = setter.build();
            } else {
                input.enabled = false;
            }
            if (getter != null) {
                getter.setReturnTypeAdapter(typeAdapter);
                input.setRefreshUrl(getter.build());
            }
            invokable.add(new BugText(typeAdapter.getUnit()));
            entry.add(BugText.NBSP);
            entry.add(invokable);
        }

        return entry;
    }
}
