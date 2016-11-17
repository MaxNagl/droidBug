package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 16.03.2015.
 */
public class StringOutput extends AbstractOutputCategory {

    public StringOutput(JavaBug javaBug) {
        super(javaBug, "string", null, 0);
    }

    @Override
    public void add(XML ul, Object o) {
        ListItemBuilder builder = new ListItemBuilder();
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
