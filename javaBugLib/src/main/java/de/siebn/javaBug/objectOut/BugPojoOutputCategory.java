package de.siebn.javaBug.objectOut;

import java.util.Map;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugList;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.BugPropertyEntryBuilder;

/**
 * Created by Sieben on 16.03.2015.
 */
public class BugPojoOutputCategory extends BugAbstractOutputCategory {
    public BugPojoOutputCategory(JavaBugCore javaBug) {
        super(javaBug, "pojo", "POJO", 1000);
    }

    @Override
    public BugElement get(Object o) {
        BugList list = new BugList();
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Map.Entry<String, AllClassMembers.POJO> pojo : allMembers.pojos.entrySet()) {
            BugPropertyEntryBuilder builder = BugPropertyEntryBuilder.getForPojo(o, pojo.getKey());
            if (builder != null) list.add(builder.build());
        }
        return list;
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        AllClassMembers allMembers = AllClassMembers.getForClass(clazz);
        return allMembers.pojos.size() > 0;
    }
}
