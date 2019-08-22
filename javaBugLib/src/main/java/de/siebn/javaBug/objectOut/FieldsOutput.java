package de.siebn.javaBug.objectOut;

import java.lang.reflect.Field;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugList;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.XML;

public class FieldsOutput extends AbstractOutputCategory {

    public FieldsOutput(JavaBug javaBug) {
        super(javaBug, "fields", "Fields", 2000);
    }

    @Override
    public BugElement get(Object o) {
        BugList list = new BugList();
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Field f : allMembers.fields) {
            list.add(getFieldInformation(o, f));
        }
        return list;
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
