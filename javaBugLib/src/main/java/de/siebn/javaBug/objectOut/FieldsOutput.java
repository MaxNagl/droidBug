package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Field;

/**
 * Created by Sieben on 16.03.2015.
 */
public class FieldsOutput extends AbstractOutputCategory {

    public FieldsOutput(JavaBug javaBug) {
        super(javaBug, "fields", "Fields", 2000);
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
