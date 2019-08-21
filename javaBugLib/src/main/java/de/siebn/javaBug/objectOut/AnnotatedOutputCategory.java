package de.siebn.javaBug.objectOut;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.util.XML;

public class AnnotatedOutputCategory implements OutputCategory {
    private final OutputMethod annotation;
    private final Class<?> clazz;
    private final Method method;

    public AnnotatedOutputCategory(OutputMethod annotation, Class<?> clazz, Method method) {
        this.annotation = annotation;
        this.clazz = clazz;
        this.method = method;
    }

    @Override
    public String getId() {
        return method.getName();
    }

    @Override
    @SuppressWarnings("TryWithIdenticalCatches")
    public void add(XML ul, Object o) {
        try {
            method.invoke(o, ul);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(BugGroup list, Object o) {
        try {
            method.invoke(o, list);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getType() {
        return method.getName();
    }

    @Override
    public String getName(Object o) {
        return annotation.value();
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return this.clazz.isAssignableFrom(clazz);
    }

    @Override
    public boolean opened(List<OutputCategory> others, boolean alreadyOpened) {
        return !alreadyOpened;
    }

    @Override
    public int getOrder() {
        return annotation.order();
    }
}
