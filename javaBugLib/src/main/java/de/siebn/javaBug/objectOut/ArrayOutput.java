package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.BugElement.BugEntry;
import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Created by Sieben on 16.03.2015.
 */
public class ArrayOutput extends AbstractOutputCategory {

    public ArrayOutput(JavaBug javaBug) {
        super(javaBug, "array", "Array", 500);
    }

    @Override
    public void add(BugGroup parent, Object o) {
        int len = Array.getLength(o);
        for (int i = 0; i < len; i++) {
            parent.add(new BugEntry().addText("[" + i + "] " + Array.get(o, i)));
        }
    }

    @Override
    public void add(XML ul, Object o) {
        int len = Array.getLength(o);
        for (int i = 0; i < len; i++) {
            ul.add("li").appendText("[" + i + "] " + Array.get(o, i));
        }
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return clazz.isArray();
    }

    @Override
    public String getName(Object o) {
        return super.getName(o) + " " + o.getClass().getComponentType().getSimpleName() + "[" + Array.getLength(o) + "]";
    }
}
