package de.siebn.javaBug.testApplication;

import java.lang.reflect.Method;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.BugElement.BugList;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.objectOut.AbstractOutputCategory;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 20.03.2015.
 */
public class TestOutputCatergory extends AbstractOutputCategory {

    public TestOutputCatergory(JavaBug javaBug) {
        super(javaBug, "test", "Test", 0);
    }

    @Override
    public BugElement get(Object o) {
        BugList list = new BugList();
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Method m : allMembers.methods) {
            if (m.getName().equals("multiply")) {
                list.add(getMethodInformation(o, m, null, null));
                list.add(getMethodInformation(o, m, null, new Object[]{2}));
                list.add(getMethodInformation(o, m, null, new Object[]{1, 2}));
                list.add(getMethodInformation(o, m, new Object[]{2}, null));
                list.add(getMethodInformation(o, m, new Object[]{2}, new Object[]{1, 2}));
            }
        }
        return list;
    }

    @Override
    public void add(XML ul, Object o) {
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Method m : allMembers.methods) {
            if (m.getName().equals("multiply")) {
                addMethodInformation(ul, o, m, null, null);
                addMethodInformation(ul, o, m, null, new Object[]{2});
                addMethodInformation(ul, o, m, null, new Object[]{1, 2});
                addMethodInformation(ul, o, m, new Object[]{2}, null);
                addMethodInformation(ul, o, m, new Object[]{2}, new Object[]{1, 2});
            }
        }
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return clazz.equals(TestClass.class);
    }
}
