package de.siebn.javaBug.util;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType.Loaded;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;

import de.siebn.javaBug.objectOut.AbstractOutputCategory.OutputMethod;

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

    public static class MethodCall {
        public List<MethodCall> calls = new ArrayList<>();
        public Object object;
        public Method method;
        public Object[] arguments;
        public Object returnValue;
        public long timeNs;
    }

    public static class ProfileCallback {
        public boolean filter(Object o, java.lang.reflect.Method method, Object[] arguments) {
            return true;
        }

        public void methodCalled(MethodCall methodCall) {

        }
    }

    @OutputMethod("profile")
    public static List<MethodCall> profile(Runnable runnable, final ProfileCallback callback) {
        final Thread thread = Thread.currentThread();
        final Stack<MethodCall> stack = new Stack<>();
        MethodCall root = new MethodCall();
        stack.add(root);
        MethodListener listener = new MethodListener() {
            @Override
            public void methodCall(Object object, java.lang.reflect.Method method, Object[] arguments) {
                if (method.getName().equals("toString") || method.getName().equals("hashCode")) return;
                if (thread != Thread.currentThread()) return;
                if (callback != null && !callback.filter(object, method, arguments)) return;
                MethodCall methodCall = new MethodCall();
                stack.peek().calls.add(methodCall);
                stack.push(methodCall);
            }

            @Override
            public void methodCalled(Object object, Method method, Object[] arguments, Object returnValue, long timeNs) {
                if (method.getName().equals("toString") || method.getName().equals("hashCode")) return;
                if (thread != Thread.currentThread()) return;
                MethodCall methodCall = stack.pop();
                methodCall.object = object;
                methodCall.method = method;
                methodCall.arguments = arguments;
                methodCall.returnValue = returnValue;
                methodCall.timeNs = timeNs;
                if (callback != null) callback.methodCalled(methodCall);
            }
        };
        try {
            BugByteCodeUtil.addMethodListener(listener);
            runnable.run();
        } finally {
            BugByteCodeUtil.removeMethodListener(listener);
        }
        return root.calls;
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
                    .method(new ElementMatcher<MethodDescription>() {
                        @Override
                        public boolean matches(MethodDescription target) {
                            System.out.println(target.getName());
                            return target.getName().equals("onMeasure") || target.getName().equals("setMeasuredDimension") || target.getName().equals("getTotalValue");
                        }
                    })
                    //.method(ElementMatchers.<MethodDescription>isPublic().or(ElementMatchers.<MethodDescription>isProtected()))
                    //.method(ElementMatchers.<MethodDescription>isPublic())
                    //.method(ElementMatchers.any())
                    //.method(ElementMatchers.<MethodDescription>named("onMeasure"))
                    .intercept(MethodDelegation.to(BugInterceptor.class))
                    .make();
            Loaded<T> loaded = CLASS_LOADING_STRATEGY == null ? unloaded.load(BugByteCodeUtil.class.getClassLoader()) : unloaded.load(BugByteCodeUtil.class.getClassLoader(), CLASS_LOADING_STRATEGY);
            buggedClasses.put(clazz, c = loaded.getLoaded());
        }
        return c;
    }

    public static <T> T getBuggedInstance(Class<T> clazz, Object... params) {
        try {
            clazz = bugClass(clazz);
            c: for (Constructor constructor : clazz.getDeclaredConstructors()) {
                Class[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length == params.length) {
                    for (int i = 0; i < params.length; i++) if (!paramTypes[i].isAssignableFrom(params[i].getClass())) continue c;
                    return (T) constructor.newInstance(params);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new IllegalArgumentException("Could not create instance of " + clazz.getName() + " with parameters: " + Arrays.toString(params));
    }
}
