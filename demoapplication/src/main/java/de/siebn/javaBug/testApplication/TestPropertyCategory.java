package de.siebn.javaBug.testApplication;

import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.objectOut.AbstractOutputCategory;
import de.siebn.javaBug.typeAdapter.TypeAdapters;

/**
 * Created by Sieben on 07.04.2015.
 */
public class TestPropertyCategory extends AbstractOutputCategory {
    public TestPropertyCategory(JavaBugCore javaBug) {
        super(javaBug, "properties", "Properties", -1);
    }

    @Property("value")
    public int setIntValue(TestClass test, Integer value, boolean set) {
        if (set)
            test.primitiveInt = value;
        return test.primitiveInt;
    }

    @Property(value = "valueMultiple", typeAdapters = {TypeAdapters.PrimitiveAdapter.class, DoubleIntAdapter.class, ChooseIntAdapter.class})
    public int setIntValueMultiple(TestClass test, Integer value, boolean set) {
        if (set)
            test.primitiveInt = value;
        return test.primitiveInt;
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return TestClass.class.isAssignableFrom(clazz);
    }
}
