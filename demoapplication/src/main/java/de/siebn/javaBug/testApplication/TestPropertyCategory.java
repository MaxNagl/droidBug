package de.siebn.javaBug.testApplication;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.objectOut.AbstractOutputCategory;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.XML;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

/**
 * Created by Sieben on 07.04.2015.
 */
public class TestPropertyCategory extends AbstractOutputCategory {
    public TestPropertyCategory(JavaBug javaBug) {
        super(javaBug, "properties", "Properties", -1);
    }

    @Property("value")
    public int setIntValue(TestClass test, Integer value, boolean set) {
        if (set)
            test.primitiveInt = value;
        return test.primitiveInt;
    }

    @Property(value = "valueMultiple", typeAdapters = {TypeAdapters.PrimitiveAdapter.class, DoubleIntAdapter.class})
    public int setIntValueMultiple(TestClass test, Integer value, boolean set) {
        if (set)
            test.primitiveInt = value;
        return test.primitiveInt;
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return clazz.equals(TestClass.class);
    }
}
