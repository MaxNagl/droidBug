package de.siebn.javaBug.testApplication;

import java.util.ArrayList;
import java.util.List;

import de.siebn.javaBug.util.BugByteCodeUtil;

public class RecursiveTestClass {
    public List<RecursiveTestClass> children = new ArrayList<>();
    public long value;

    public long getTotalValue() {
        long v = value;
        for (RecursiveTestClass child : children) {
            v += child.getTotalValue();
        }
        return v;
    }

    public static RecursiveTestClass getBuggedTestHierarchy() {
        RecursiveTestClass r = BugByteCodeUtil.getBuggedInstance(RecursiveTestClass.class);
        r.value = 1;
        RecursiveTestClass r1 = BugByteCodeUtil.getBuggedInstance(RecursiveTestClass.class);
        r.children.add(r1);
        r1.value = 2;
        RecursiveTestClass r2 = BugByteCodeUtil.getBuggedInstance(RecursiveTestClass.class);
        r.children.add(r2);
        r2.value = 2;
        RecursiveTestClass r3 = BugByteCodeUtil.getBuggedInstance(RecursiveTestClass.class);
        r.children.add(r3);
        r3.value = 2;
        RecursiveTestClass r11 = BugByteCodeUtil.getBuggedInstance(RecursiveTestClass.class);
        r1.children.add(r11);
        r11.value = 3;
        RecursiveTestClass r12 = BugByteCodeUtil.getBuggedInstance(RecursiveTestClass.class);
        r1.children.add(r12);
        r12.value = 3;
        return r;
    }
}
