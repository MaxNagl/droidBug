package de.siebn.javaBug.objectOut;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.BugElement.BugList;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.plugins.ObjectBugPlugin.InvocationLinkBuilder;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;
import de.siebn.javaBug.util.BugReflectionUtils;
import de.siebn.javaBug.util.BugPropertyEntryBuilder;

public abstract class BugSimpleOutputCategory<T> extends BugAbstractOutputCategory {

    public interface SimpleProperty<O, V> {
        String getName();

        Class<? extends O> getClazz();

        boolean isSettable();

        List<TypeAdapter<Object>> getTypeAdapters();

        V getValue(O object);

        void setValue(O object, V value);
    }

    public static abstract class AbstractProperty<O, V> implements SimpleProperty<O, V> {
        protected String name;
        protected Class clazz;
        protected boolean settable;
        protected List<TypeAdapter<Object>> typeAdapters;

        public AbstractProperty(String name, Class clazz, boolean settable) {
            this.name = name;
            this.clazz = clazz;
            this.settable = settable;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends O> getClazz() {
            return clazz;
        }

        @Override
        public boolean isSettable() {
            return settable;
        }

        @SuppressWarnings("unchecked")
        public AbstractProperty setTypeAdapters(TypeAdapter... typeAdapters) {
            this.typeAdapters = (List) Arrays.asList(typeAdapters);
            return this;
        }

        public AbstractProperty setTypeAdapters(List<TypeAdapter<Object>> typeAdapters) {
            this.typeAdapters = typeAdapters;
            return this;
        }

        @Override
        public List<TypeAdapter<Object>> getTypeAdapters() {
            return typeAdapters;
        }
    }

    @SuppressWarnings("unchecked")
    public class DelegateProperty<O, V extends T> extends AbstractProperty<O, V> {
        public DelegateProperty(String name, Class clazz, boolean settable) {
            super(name, clazz, settable);
        }

        @Override
        public V getValue(O object) {
            return (V) ((BugSimpleOutputCategory) BugSimpleOutputCategory.this).getValue((Object) object, this);
        }

        @Override
        public void setValue(O object, V value) {
            BugSimpleOutputCategory.this.setValue((T) object, this, value);
        }
    }

    public static class FieldProperty extends AbstractProperty {
        private Field field;

        public FieldProperty(Field field) {
            super(field.getName(), field.getType(), !Modifier.isFinal(field.getModifiers()));
            this.field = field;
        }

        public FieldProperty(Class clazz, String fieldName) {
            this(BugReflectionUtils.getFieldOrThrow(clazz, fieldName));
        }

        @Override
        public Object getValue(Object object) {
            return BugReflectionUtils.getOrNull(object, field);
        }

        @Override
        public void setValue(Object object, Object value) {
            BugReflectionUtils.set(object, field, value);
        }
    }

    public static class FieldsProperty extends AbstractProperty {
        private Field[] fields;

        public FieldsProperty(Field... fields) {
            super(fields[fields.length - 1].getName(), fields[fields.length - 1].getType(), !Modifier.isFinal(fields[fields.length - 1].getModifiers()));
            this.fields = fields;
        }

        @Override
        public Object getValue(Object object) {
            for (Field field : fields)
                object = BugReflectionUtils.getOrNull(object, field);
            return object;
        }

        @Override
        public void setValue(Object object, Object value) {
            for (int i = 0; i < fields.length - 1; i++)
                object = BugReflectionUtils.getOrNull(object, fields[i]);
            BugReflectionUtils.set(object, fields[fields.length - 1], value);
        }
    }

    public BugSimpleOutputCategory(JavaBugCore javaBug, String type, String name, int order) {
        super(javaBug, type, name, order);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BugElement get(Object o) {
        BugList list = new BugList();
        addElements(list, (T) o);
        return list;
    }

    protected abstract void addElements(BugGroup parent, T o);

    public void addProperty(BugGroup parent, T object, SimpleProperty property) {
        Object value = BugReflectionUtils.invokeOrNull(property, getValueMethod, object);
        parent.add(new BugPropertyEntryBuilder()
                .setName(property.getName())
                .setValue(value)
                .setClazz(property.getClazz())
                .setParamIndex(1)
                .setGetter(new InvocationLinkBuilder().setMethod(getValueMethod).setObject(property).setPredefined(0, object))
                .setSetter(property.isSettable() ? new InvocationLinkBuilder().setMethod(setValueMethod).setObject(property).setPredefined(0, object).setParameterClazz(1, property.getClazz()) : null)
                .setTypeAdapters(property.getTypeAdapters())
                .build());
    }

    protected Object getValue(T object, SimpleProperty property) {
        return null;
    }

    protected void setValue(T object, SimpleProperty property, Object value) {
    }

    private static Method getValueMethod = BugReflectionUtils.getMethodOrNull(SimpleProperty.class, "getValue", Object.class);
    private static Method setValueMethod = BugReflectionUtils.getMethodOrNull(SimpleProperty.class, "setValue", Object.class, Object.class);
}
