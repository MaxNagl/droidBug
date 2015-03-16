package de.siebn.javaBug.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by Sieben on 16.03.2015.
 */
public class AllClassMembers {
    private static final HashMap<Class<?>, AllClassMembers> allMembersMap = new HashMap<>();

    public final ArrayList<Field> fields = new ArrayList<>();
    public final ArrayList<Method> methods = new ArrayList<>();
    public final ArrayList<Method> pojo = new ArrayList<>();

    private AllClassMembers(Class<?> clazz) {
        addAllMembers(clazz);
        Collections.sort(fields, new Comparator<Field>() { public int compare(Field o1, Field o2) { return o1.getName().compareTo(o2.getName());}});
        Collections.sort(methods, new Comparator<Method>() {@Override public int compare(Method o1, Method o2) {return o1.getName().compareTo(o2.getName());}});
        Collections.sort(pojo, new Comparator<Method>() {@Override public int compare(Method o1, Method o2) {return o1.getName().compareTo(o2.getName());}});
    }

    private void addAllMembers(Class<?> clazz) {
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            fields.add(f);
        }
        for (Method m : clazz.getDeclaredMethods()) {
            m.setAccessible(true);
            methods.add(m);
            if (!clazz.equals(Object.class)) {
                if (m.getName().startsWith("get") && m.getName().length() > 3 && m.getParameterTypes().length == 0) pojo.add(m);
                if (m.getName().startsWith("set") && m.getName().length() > 3 && m.getParameterTypes().length == 1) pojo.add(m);
            }
        }
        if (clazz.getSuperclass() != null) addAllMembers(clazz.getSuperclass());
    }

    public static AllClassMembers getForClass(Class<?> clazz) {
        AllClassMembers allMembers = allMembersMap.get(clazz);
        if (allMembers == null)
            allMembersMap.put(clazz, allMembers = new AllClassMembers(clazz));
        return allMembers;
    }

}
