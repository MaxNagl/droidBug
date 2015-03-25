package de.siebn.javaBug.objectOut;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by Sieben on 16.03.2015.
 */
public class PojoOutput extends AbstractOutputCategory {
    public PojoOutput(JavaBug javaBug) {
        super(javaBug, "pojo", "POJO", 1000);
    }

    @Override
    public void add(XML ul, Object o) {
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Map.Entry<String, AllClassMembers.POJO> pojo : allMembers.pojos.entrySet()) {
            addPojo(ul, o, pojo.getKey());
            //addMethodInformation(ul, o, m, null, null);
        }
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        AllClassMembers allMembers = AllClassMembers.getForClass(clazz);
        return allMembers.pojos.size() > 0;
    }
}
