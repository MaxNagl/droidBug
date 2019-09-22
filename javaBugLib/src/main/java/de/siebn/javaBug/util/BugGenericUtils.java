package de.siebn.javaBug.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BugGenericUtils {
    public static Object invokeOrNull(Object object, Method method, Object... args) {
        try {
            return method.invoke(object, args);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object getOrNull(Object object, Field field) {
        try {
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

    public static Method getMethodOrNull(Class clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Method getOrNullMethod = getMethodOrNull(BugGenericUtils.class, "getOrNull", Object.class, Field.class);
    public static Method setMethod = getMethodOrNull(BugGenericUtils.class, "set", Object.class, Field.class, Object.class);
}
