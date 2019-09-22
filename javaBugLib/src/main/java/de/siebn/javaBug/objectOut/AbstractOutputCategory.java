package de.siebn.javaBug.objectOut;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.plugins.ObjectBugPlugin.InvocationLinkBuilder;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;
import de.siebn.javaBug.util.*;
import de.siebn.javaBug.util.BugByteCodeUtil.MethodCall;

public abstract class AbstractOutputCategory implements OutputCategory {
    private final static Object[] empty = new Object[0];
    protected final JavaBugCore javaBug;
    protected final String type;
    protected final String name;
    protected int order;

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Property {
        String value();

        Class<?>[] typeAdapters() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface OutputMethod {
        String value();
    }

    public AbstractOutputCategory(JavaBugCore javaBug, String type, String name, int order) {
        this.javaBug = javaBug;
        this.type = type;
        this.name = name;
        this.order = order;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getName(Object o) {
        return name;
    }

    @Override
    public boolean opened(List<OutputCategory> others, boolean alreadyOpened) {
        return !alreadyOpened;
    }

    @Override
    public int getOrder() {
        return order;
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

    public BugElement getMethodInformation(Object o, Method m, Object[] predefined, Object[] preset) {
        if (predefined == null) predefined = empty;
        if (preset == null) preset = empty;
        boolean canInvoke = true;
        Class<?>[] parameterTypes = m.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class c = parameterTypes[i];
            if (!TypeAdapters.getTypeAdapter(c).canParse(c) && (predefined.length <= i || predefined[i] == null))
                canInvoke = false;
        }
        BugEntry json = new BugEntry();
        json.addClazz(StringifierUtil.modifiersToString(m.getModifiers(), "mod", true));
        json.add(BugText.getForModifier(m.getModifiers())).addSpace();
        json.add(BugText.getForClass(m.getReturnType())).addSpace();
        json.add(BugText.getForMethod(m).setOnClick(BugText.ON_CLICK_EXPAND));

        BugInvokable invokable = new BugInvokable(BugInvokable.ACTION_EXPAND_RESULT);
        boolean firstParameter = true;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (predefined.length > i && predefined[i] != null) continue;
            if (!firstParameter) invokable.add(new BugText(", "));
            invokable.add(BugText.getForClass(parameterTypes[i])).addSpace();
            invokable.add(BugInputElementBuilder.build(preset.length > i ? preset[i] : null, parameterTypes[i], i, TypeAdapters.getTypeAdapter(parameterTypes[i])));
            firstParameter = false;
        }
        if (canInvoke) invokable.url = new InvocationLinkBuilder(m, o).setReturnType(ObjectBugPlugin.RETURN_TYPE_JSON).setPredefined(predefined).build();
        invokable.addBraces();
        invokable.add(BugText.INVOKER);
        json.add(invokable);
        return json;
    }

    public BugElement getProfileElement(List<MethodCall> calls) {
        BugList list = new BugList();
        for (MethodCall call : calls) addMethodCall(list, call);
        return list;
    }

    public void addMethodCall(BugGroup group, MethodCall methodCall) {
        BugEntry entry = new BugEntry().setAutoExpand(true);
        entry.add(BugText.getForValueFormated(methodCall.object, BugFormat.colorPrimary));
        entry.addText(".").add(BugText.getForMethod(methodCall.method));
        entry.addText("(");
        boolean firstArg = true;
        for (Object arg : methodCall.arguments) {
            if (!firstArg) entry.addText(", ");
            entry.add(BugText.getForValue(arg));
            firstArg = false;
        }
        entry.addText(") -> ");
        entry.add(BugText.getForValueFormated(methodCall.returnValue));
        entry.addText(" (" + StringifierUtil.nanoSecondsToString(methodCall.timeNs) + ")");
        BugList children = new BugList();
        for (MethodCall call : methodCall.calls)
            addMethodCall(children, call);
        entry.setExpand(children);
        group.add(entry);
    }
}
