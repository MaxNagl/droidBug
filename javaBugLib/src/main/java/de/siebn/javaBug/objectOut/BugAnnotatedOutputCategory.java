package de.siebn.javaBug.objectOut;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugList;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.BugPropertyEntryBuilder;

public abstract class BugAnnotatedOutputCategory extends BugAbstractOutputCategory {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Property {
        String value();

        Class<?>[] typeAdapters() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface OutputMethod {
        String value();
    }

    public BugAnnotatedOutputCategory(JavaBugCore javaBug, String type, String name, int order) {
        super(javaBug, type, name, order);
    }

    @Override
    public BugElement get(Object o) {
        BugList list = new BugList();
        for (Method m : AllClassMembers.getForClass(getClass()).methods) {
            Property getterSetter = m.getAnnotation(Property.class);
            if (getterSetter != null && showGetterSetter(o, m)) {
                list.add(BugPropertyEntryBuilder.getForGetterSetter(getterSetter.value(), this, m, o, TypeAdapters.getTypeAdapterClasses(getterSetter.typeAdapters())).build());
            }
            OutputMethod outputMethod = m.getAnnotation(OutputMethod.class);
            if (outputMethod != null && showOutputMethod(o, m)) {
                list.add(getMethodInformation(this, m, new Object[]{o}, null));
            }
        }
        return list;
    }

    protected boolean showOutputMethod(Object o, Method method) {
        return true;
    }

    protected boolean showGetterSetter(Object o, Method method) {
        return true;
    }
}
