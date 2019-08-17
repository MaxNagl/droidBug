package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.JsonBugList;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Method;

/**
 * Created by Sieben on 16.03.2015.
 */
public class MethodsOutput extends AbstractOutputCategory {
    public MethodsOutput(JavaBug javaBug) {
        super(javaBug, "methods", "Methods", 3000);
    }

    @Override
    public void add(JsonBugList list, Object o) {
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Method m : allMembers.methods) {
            addMethodInformation(list, o, m, null, null);
        }
    }

    @Override
    public void add(XML ul, Object o) {
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Method m : allMembers.methods) {
            addMethodInformation(ul, o, m, null, null);
        }
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return true;
    }

}
