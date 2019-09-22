package de.siebn.javaBug.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BugGenericUtils {
    public static Object invokeOrNull(Object object, Method method, Object... args) {
        try {
            if (!method.isAccessible()) method.setAccessible(true);
            return method.invoke(object, args);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object getOrNull(Object object, Field field) {
        try {
            if (!field.isAccessible()) field.setAccessible(true);
            return field.get(object);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void set(Object object, Field field, Object value) {
        try {
            field.set(object, value);
        } catch (Throwable t) {
        }
    }

    @SuppressWarnings("unchecked")
    public static Method getMethodOrNull(Class clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Field getFieldOrNull(Class clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Field getFieldOrThrow(Class clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (Throwable t) {
            throw  new RuntimeException(t);
        }
    }

    public static Method getOrNullMethod = getMethodOrNull(BugGenericUtils.class, "getOrNull", Object.class, Field.class);
    public static Method setMethod = getMethodOrNull(BugGenericUtils.class, "set", Object.class, Field.class, Object.class);
}
