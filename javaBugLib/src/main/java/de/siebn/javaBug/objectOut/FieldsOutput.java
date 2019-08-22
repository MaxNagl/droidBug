package de.siebn.javaBug.objectOut;

import java.lang.reflect.Field;

import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.XML;

public class FieldsOutput extends AbstractOutputCategory {

    public FieldsOutput(JavaBug javaBug) {
        super(javaBug, "fields", "Fields", 2000);
    }

    @Override
    public void add(BugGroup list, Object o) {
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Field f : allMembers.fields) {
            list.add(getFieldInformation(o, f));
        }
    }

    @Override
    public void add(XML ul, Object o) {
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Field f : allMembers.fields) {
            addFieldInformation(ul, o, f);
        }
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return true;
    }
}
