package de.siebn.javaBug.objectOut;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.BugElement.BugList;
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
    public BugElement get(Object o) {
        try {
            return (BugElement) method.invoke(o);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
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
