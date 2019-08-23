package de.siebn.javaBug.objectOut;

import java.util.Map;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.BugElement.BugList;
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
    public BugElement get(Object o) {
        BugList list = new BugList();
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Map.Entry<String, AllClassMembers.POJO> pojo : allMembers.pojos.entrySet()) {
            list.add(getPojo(o, pojo.getKey()));
        }
        return list;
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        AllClassMembers allMembers = AllClassMembers.getForClass(clazz);
        return allMembers.pojos.size() > 0;
    }
}
