package de.siebn.javaBug.testApplication;

import java.lang.reflect.Method;
import java.time.DayOfWeek;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugList;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.objectOut.BugAbstractOutputCategory;
import de.siebn.javaBug.util.AllClassMembers;

/**
 * Created by Sieben on 20.03.2015.
 */
public class TestOutputCatergory extends BugAbstractOutputCategory {

    public TestOutputCatergory(JavaBugCore javaBug) {
        super(javaBug, "testAbstract", "Test Abstract", 0);
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
            if (m.getName().equals("setDay")) {
                list.add(getMethodInformation(o, m, null, null));
                list.add(getMethodInformation(o, m, null, new Object[]{DayOfWeek.FRIDAY}));
                list.add(getMethodInformation(o, m, new Object[]{DayOfWeek.WEDNESDAY}, null));
            }
        }
        return list;
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return TestClass.class.isAssignableFrom(clazz);
    }
}
