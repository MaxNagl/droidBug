package de.siebn.javaBug.util;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Loaded;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;

@SuppressWarnings("unchecked")
public class BugByteCodeUtil {
    public static ClassLoadingStrategy CLASS_LOADING_STRATEGY;
    private static Boolean available;
    private static HashMap<Class, Class> buggedClasses = new HashMap<>();

    private static Set<MethodListener> methodListeners;

    public interface MethodListener {
        void methodCall(Object object, Method method, Object[] arguments);

        void methodCalled(Object object, Method method, Object[] arguments, Object returnValue, long timeNs);
    }

    public static synchronized void addMethodListener(MethodListener listener) {
        if (methodListeners == null) methodListeners = new CopyOnWriteArraySet<>();
        methodListeners.add(listener);
    }

    public static synchronized void removeMethodListener(MethodListener listener) {
        if (methodListeners == null) return;
        methodListeners.remove(listener);
        if (methodListeners.size() == 0) methodListeners = null;
    }

    public static boolean isAvailable() {
        if (available == null) {
            try {
                new ByteBuddy();
                available = false;
            } catch (Exception e) {
                available = false;
            }
        }
        return available;
    }

    public static class BugInterceptor {
        @RuntimeType
        public static Object intercept(
                @This Object object,
                @Origin Method method,
                @AllArguments Object[] arguments,
                @SuperCall Callable<?> callable
        ) throws Exception {
            if (methodListeners != null) {
                Set<MethodListener> methodListeners = BugByteCodeUtil.methodListeners;
                for (MethodListener listener : methodListeners) {
                    listener.methodCall(object, method, arguments);
                }
                long start = System.nanoTime();
                Object returnValue = null;
                try {
                    return returnValue = callable.call();
                } finally {
                    long timeNs = System.nanoTime() - start;
                    for (MethodListener listener : methodListeners) {
                        listener.methodCalled(object, method, arguments, returnValue, timeNs);
                    }
                }
            }
            return callable.call();
        }
    }

    public static <T> Class<T> bugClass(Class<T> clazz) {
        Class c = buggedClasses.get(clazz);
        if (c == null) {
            Unloaded<T> unloaded = new ByteBuddy().with(TypeValidation.DISABLED).subclass(clazz)
                    .name(clazz.getName() + "$Bugged")
                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(BugInterceptor.class))
                    .make();
            Loaded<T> loaded = CLASS_LOADING_STRATEGY == null ? unloaded.load(BugByteCodeUtil.class.getClassLoader()) : unloaded.load(BugByteCodeUtil.class.getClassLoader(), CLASS_LOADING_STRATEGY);
            buggedClasses.put(clazz, c = loaded.getLoaded());
        }
        return c;
    }

    public static <T> T getBuggedInstance(Class<T> clazz) {
        try {
            return bugClass(clazz).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
