package de.siebn.javaBug.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

public interface BugProperty {
    Object getValue(Object object);

    void setValue(Object object, Object value);

    Class getType();

    boolean isReadable();

    boolean isSettable();

    String getName();

    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    class BugFieldProperty implements BugProperty {
        public final Field field;

        public BugFieldProperty(Field field) {
            this.field = field;
        }

        @Override
        public Object getValue(Object object) {
            return BugReflectionUtils.getOrNull(object, field);
        }

        @Override
        public void setValue(Object object, Object value) {
            BugReflectionUtils.set(object, field, value);
        }

        @Override
        public Class getType() {
            return field.getType();
        }

        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public boolean isSettable() {
            return !Modifier.isFinal(field.getModifiers());
        }

        @Override
        public String getName() {
            return field.getName();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return field.getAnnotation(annotationClass);
        }

        public static BugFieldProperty get(Class clazz, String fieldName) {
            return new BugFieldProperty(BugReflectionUtils.getFieldOrThrow(clazz, fieldName));
        }
    }

    class BugMethodProperty implements BugProperty {
        public final Method getter;
        public final Method setter;
        public final String name;

        public BugMethodProperty(Method getter, Method setter) {
            this.getter = getter;
            this.setter = setter;
            this.name = null;
        }

        public BugMethodProperty(Method getter, Method setter, String name) {
            this.getter = getter;
            this.setter = setter;
            this.name = name;
        }

        @Override
        public Object getValue(Object object) {
            return BugReflectionUtils.invokeOrNull(object, getter);
        }

        @Override
        public void setValue(Object object, Object value) {
            BugReflectionUtils.invokeOrNull(object, setter, value);
        }

        @Override
        public Class getType() {
            return getter != null ? getter.getReturnType() : setter.getParameterTypes()[0];
        }

        @Override
        public boolean isReadable() {
            return getter != null;
        }

        @Override
        public boolean isSettable() {
            return setter != null;
        }

        @Override
        public String getName() {
            if (name != null) return name;
            return getter != null ? getter.getName() : setter.getName();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return getter != null ? getter.getAnnotation(annotationClass) : setter.getAnnotation(annotationClass);
        }

        public static BugProperty getForPojo(Method getter, Method setter) {
            String name = null;
            if (getter != null && getter.getName().startsWith("get")) name = getter.getName().substring(3);
            else if (setter != null && setter.getName().startsWith("set")) name = setter.getName().substring(3);
            if (name != null) name = name.substring(0, 1).toLowerCase() + name.substring(1);
            return new BugMethodProperty(getter, setter, name);
        }
    }

    class BugChainProperty implements BugProperty {
        public final BugProperty[] properties;

        public BugChainProperty(BugProperty... properties) {
            this.properties = properties;
        }

        @Override
        public Object getValue(Object object) {
            for (BugProperty property : properties)
                object = property.getValue(object);
            return object;
        }

        @Override
        public void setValue(Object object, Object value) {
            for (int i = 0; i < properties.length - 1; i++)
                object = properties[i].getValue(object);
            properties[properties.length - 1].setValue(object, value);
        }

        @Override
        public Class getType() {
            return properties[properties.length - 1].getType();
        }

        @Override
        public boolean isReadable() {
            return properties[properties.length - 1].isReadable();
        }

        @Override
        public boolean isSettable() {
            return properties[properties.length - 1].isSettable();
        }

        @Override
        public String getName() {
            return properties[properties.length - 1].getName();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return properties[properties.length - 1].getAnnotation(annotationClass);
        }
    }
}
