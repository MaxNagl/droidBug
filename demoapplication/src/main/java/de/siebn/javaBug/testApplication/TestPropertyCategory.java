package de.siebn.javaBug.testApplication;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.objectOut.AbstractOutputCategory;
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

    @Retention(RetentionPolicy.RUNTIME)
    @interface Property {
        String value();
    }

    @Override
    public void add(XML ul, Object o) {
        for (Method m : AllClassMembers.getForClass(getClass()).methods) {
            Property getterSetter = m.getDeclaredAnnotation(Property.class);
            if (getterSetter != null) {
                addProperty(ul, getterSetter.value(), o, m);
            }
        }
    }

    @Property("value")
    public int setIntValue(TestClass test, Integer value, boolean set) {
        if (set)
            test.primitiveInt = value;
        return test.primitiveInt;
    }

    public void addProperty(XML ul, String name, Object o, Method setter) {
        XML li = ul.add("li").setClass("object notOpenable");
        li.add("span").setClass("fieldName").appendText(name);
        li.appendText(": ");
        Object val = null;
        try {
            val = setter.invoke(this, o, null, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        XML p = li.add("span").setClass("parameter").appendText(String.valueOf(val));
        p.setAttr("editurl", javaBug.getObjectBug().getInvokationLink(false, this, setter, o, null, true));
        p.setAttr("param", "p1");
        if (!setter.getReturnType().isPrimitive())
            p.setAttr("editNullify", "true");
    }

//    AllClassMembers.POJO pojo = AllClassMembers.getForClass(o.getClass()).pojos.get(field);
//    if (pojo == null) return;
//    boolean setable = pojo.setter != null && TypeAdapters.canParse(pojo.setter.getParameterTypes()[0]);
//    if (!setable && pojo.getter == null) return;
//    XML li = ul.add("li").setClass("object notOpenable");
//    li.appendText(" ").add("span").setClass("fieldName").appendText(field);
//    li.appendText(": ");
//    Object val = null;
//    if (pojo.getter != null) {
//        try {
//            val = pojo.getter.invoke(o);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
//    }
//    XML p = li.add("span").setClass("parameter").appendText(String.valueOf(val));
//    if (setable) {
//        p.setAttr("editurl", javaBug.getObjectBug().getPojoLink(o, field));
//        if (!pojo.setter.getParameterTypes()[0].isPrimitive())
//            p.setAttr("editNullify", "true");
//    }


    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return clazz.equals(TestClass.class);
    }
}
