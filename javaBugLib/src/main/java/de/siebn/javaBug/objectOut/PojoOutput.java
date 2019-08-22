package de.siebn.javaBug.objectOut;

import java.util.Map;

import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 16.03.2015.
 */
public class PojoOutput extends AbstractOutputCategory {
    public PojoOutput(JavaBug javaBug) {
        super(javaBug, "pojo", "POJO", 1000);
    }

    @Override
    public void add(BugGroup list, Object o) {
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Map.Entry<String, AllClassMembers.POJO> pojo : allMembers.pojos.entrySet()) {
            list.add(getPojo(o, pojo.getKey()));
        }
    }

    @Override
    public void add(XML ul, Object o) {
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Map.Entry<String, AllClassMembers.POJO> pojo : allMembers.pojos.entrySet()) {
            addPojo(ul, o, pojo.getKey());
        }
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        AllClassMembers allMembers = AllClassMembers.getForClass(clazz);
        return allMembers.pojos.size() > 0;
    }
}
