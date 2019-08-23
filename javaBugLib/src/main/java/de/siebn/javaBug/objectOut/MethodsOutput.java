package de.siebn.javaBug.objectOut;

import java.lang.reflect.Method;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.BugElement.BugList;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 16.03.2015.
 */
public class MethodsOutput extends AbstractOutputCategory {
    public MethodsOutput(JavaBug javaBug) {
        super(javaBug, "methods", "Methods", 3000);
    }

    @Override
    public BugElement get(Object o) {
        BugList list = new BugList();
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Method m : allMembers.methods) {
            list.add(getMethodInformation( o, m, null, null));
        }
        return list;
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return true;
    }

}
