package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by Sieben on 16.03.2015.
 */
public class StringOutput extends AbstractOutputCategory {

    public StringOutput(JavaBug javaBug) {
        super(javaBug, "string", null, 0);
    }

    @Override
    public void add(XML ul, Object o) {
        PropertyBuilder builder = new PropertyBuilder();
        builder.setName(TypeAdapters.toString(o));
        builder.setType(o.getClass());
        builder.setExpandLink(javaBug.getObjectBug().getObjectDetailsLink(o));
        builder.build(ul);
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return true;
    }
}
