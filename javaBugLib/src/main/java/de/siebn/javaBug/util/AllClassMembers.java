package de.siebn.javaBug.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by Sieben on 16.03.2015.
 */
public class AllClassMembers {
    private static final HashMap<Class<?>, AllClassMembers> allMembersMap = new HashMap<>();

    public final ArrayList<Field> fields = new ArrayList<>();
    public final ArrayList<Method> methods = new ArrayList<>();
    public final Map<String, POJO> pojos = new TreeMap();

    public class POJO {
        public Method getter;
        public Method setter;
    }

    private AllClassMembers(Class<?> clazz) {
        addAllMembers(clazz);
        Collections.sort(fields, new Comparator<Field>() { public int compare(Field o1, Field o2) { return o1.getName().compareTo(o2.getName());}});
        Collections.sort(methods, new Comparator<Method>() {@Override public int compare(Method o1, Method o2) {return o1.getName().compareTo(o2.getName());}});
    }

    private void addAllMembers(Class<?> clazz) {
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            fields.add(f);
        }
        m: for (Method m : clazz.getDeclaredMethods()) {
            for (Method om : methods) {
                if (om.getName().equals(m.getName()) && om.getReturnType().equals(m.getReturnType()) && Arrays.equals(m.getParameterTypes(), om.getParameterTypes())) continue m;
            }
            m.setAccessible(true);
            methods.add(m);
            if (!clazz.equals(Object.class) && m.getName().length() > 3) {
                String field = m.getName().substring(3);
                if (m.getName().startsWith("get") && m.getParameterTypes().length == 0) {
                    POJO pojo = pojos.get(field);
                    if (pojo == null) pojos.put(field, pojo = new POJO());
                    pojo.getter = m;
                }
                if (m.getName().startsWith("set") && m.getParameterTypes().length == 1) {
                    POJO pojo = pojos.get(field);
                    if (pojo == null) pojos.put(field, pojo = new POJO());
                    pojo.setter = m;
                }
            }
        }
        if (clazz.getSuperclass() != null) addAllMembers(clazz.getSuperclass());
    }

    public Field getField(String identifier) {
        int lastDot = identifier.lastIndexOf(".");
        if (lastDot > 0) {
            String fieldName = identifier.substring(lastDot + 1);
            String className = identifier.substring(0, lastDot);
            for (Field f : fields) {
                if (f.getName().equals(fieldName) && f.getDeclaringClass().getCanonicalName().equals(className)) return f;
            }
        } else {
            for (Field f : fields) {
                if (f.getName().equals(identifier)) return f;
            }
        }
        return null;
    }

    public String getFieldIdentifier(Field field) {
        String fieldName = field.getName();
        for (Field f : fields) {
            if (f == field) return f.getName();
            if (f.getName().equals(fieldName)) {
                return field.getDeclaringClass().getCanonicalName() + "." + field.getName();
            }
        }
        return fieldName;
    }

    public static AllClassMembers getForClass(Class<?> clazz) {
        AllClassMembers allMembers = allMembersMap.get(clazz);
        if (allMembers == null)
            allMembersMap.put(clazz, allMembers = new AllClassMembers(clazz));
        return allMembers;
    }

}
