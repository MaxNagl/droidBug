package de.siebn.javaBug.testApplication;

import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.objectOut.BugSimpleOutputCategory;
import de.siebn.javaBug.typeAdapter.TypeAdapters;

public class TestSimpleOutputCategory extends BugSimpleOutputCategory<TestClass> {
    private SimpleProperty ArrayFirst = new DelegateProperty("ArrayFirst", String.class, true);
    private SimpleProperty ArraySecond = new DelegateProperty("ArraySecond", String.class, false);
    private SimpleProperty ArrayThird = new DelegateProperty("ArrayThird", String.class, true);
    private SimpleProperty Value = new FieldProperty(TestClass.class, "primitiveInt").setTypeAdapters(new TypeAdapters.PrimitiveAdapter(), new DoubleIntAdapter(), new ChooseIntAdapter());

    public TestSimpleOutputCategory(JavaBugCore javaBug) {
        super(javaBug, "testSimple", "Test Simple", 0);
    }

    @Override
    protected void addElements(BugGroup parent, TestClass o) {
        addProperty(parent, o, ArrayFirst);
        addProperty(parent, o, ArraySecond);
        addProperty(parent, o, ArrayThird);
        addProperty(parent, o, Value);
    }

    @Override
    protected Object getValue(TestClass object, SimpleProperty property) {
        if (property == ArrayFirst) return object.arrayString[0];
        if (property == ArraySecond) return object.arrayString[1];
        if (property == ArrayThird) return object.arrayString[2];
        return null;
    }

    @Override
    protected void setValue(TestClass object, SimpleProperty property, Object value) {
        if (property == ArrayFirst) object.arrayString[0] = (String) value;
        if (property == ArraySecond) object.arrayString[1] = (String) value;
        if (property == ArrayThird) object.arrayString[2] = (String) value;
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return TestClass.class.isAssignableFrom(clazz);
    }
}
