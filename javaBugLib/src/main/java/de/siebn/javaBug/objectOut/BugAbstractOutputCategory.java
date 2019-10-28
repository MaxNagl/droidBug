package de.siebn.javaBug.objectOut;

import java.lang.reflect.Method;
import java.util.List;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.plugins.ObjectBugPlugin.InvocationLinkBuilder;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.BugByteCodeUtil.MethodCall;
import de.siebn.javaBug.util.BugInputElementBuilder;
import de.siebn.javaBug.util.StringifierUtil;

public abstract class BugAbstractOutputCategory implements BugOutputCategory {
    private final static Object[] empty = new Object[0];
    protected final JavaBugCore javaBug;
    protected final String type;
    protected final String name;
    protected int order;

    public BugAbstractOutputCategory(JavaBugCore javaBug, String type, String name, int order) {
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
    public boolean opened(List<BugOutputCategory> others, boolean alreadyOpened) {
        return !alreadyOpened;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public BugElement getMethodInformation(Object o, Method m, Object[] predefined, Object[] preset) {
        if (predefined == null) predefined = empty;
        if (preset == null) preset = empty;
        Class<?>[] parameterTypes = m.getParameterTypes();
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
        invokable.url = new InvocationLinkBuilder(m, o).setReturnType(ObjectBugPlugin.RETURN_TYPE_JSON).setPredefined(predefined).build();
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
