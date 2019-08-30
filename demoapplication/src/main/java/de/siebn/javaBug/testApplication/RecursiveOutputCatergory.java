package de.siebn.javaBug.testApplication;

import java.lang.reflect.Method;
import java.util.*;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.objectOut.AbstractOutputCategory;
import de.siebn.javaBug.util.BugByteCodeUtil;
import de.siebn.javaBug.util.BugByteCodeUtil.MethodListener;
import de.siebn.javaBug.util.StringifierUtil;

/**
 * Created by Sieben on 20.03.2015.
 */
public class RecursiveOutputCatergory extends AbstractOutputCategory {

    public RecursiveOutputCatergory(JavaBug javaBug) {
        super(javaBug, "recursion", "Recursion", 0);
    }

    private static class MethodCall {
        public List<MethodCall> calls = new ArrayList<>();
        public Object object;
        public Method method;
        public Object[] arguments;
        public Object returnValue;
        public long timeNs;
    }

    @OutputMethod("profile")
    public BugElement profile(RecursiveTestClass o) {
        Stack<MethodCall> stack = new Stack<>();
        stack.add(new MethodCall());
        Thread thread = Thread.currentThread();
        MethodListener listener = new MethodListener() {
            @Override
            public void methodCall(Object object, java.lang.reflect.Method method, Object[] arguments) {
                if (method.getName().equals("toString") || method.getName().equals("hashCode")) return;
                if (thread != Thread.currentThread()) return;
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
            }
        };
        try {
            BugByteCodeUtil.addMethodListener(listener);
            o.getTotalValue();
        } finally {
            BugByteCodeUtil.removeMethodListener(listener);
        }
        BugList list = new BugList();
        addMethodCall(list, stack.pop().calls.get(0));
        return list;
    }

    public void addMethodCall(BugGroup group, MethodCall methodCall) {
        BugEntry entry = new BugEntry().setAutoExpand(true);
        entry.add(BugText.getForValue(methodCall.object));
        entry.addText(".").addText(methodCall.method.getName());
        entry.addText("(");
        boolean firstArg = true;
        for (Object arg : methodCall.arguments) {
            if (!firstArg) entry.addText(", ");
            entry.add(BugText.getForValue(arg));
            firstArg = false;
        }
        entry.addText(") -> ");
        entry.add(BugText.getForValue(methodCall.returnValue));
        entry.addText(" (" + StringifierUtil.nanoSecondsToString(methodCall.timeNs) + ")");
        BugList children = new BugList();
        for (MethodCall call : methodCall.calls)
            addMethodCall(children, call);
        entry.setExpand(children);
        group.add(entry);
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return RecursiveTestClass.class.isAssignableFrom(clazz);
    }
}
