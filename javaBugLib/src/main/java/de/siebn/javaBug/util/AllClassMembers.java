package de.siebn.javaBug.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import de.siebn.javaBug.util.BugProperty.BugFieldProperty;
import de.siebn.javaBug.util.BugProperty.BugMethodProperty;

/**
 * Created by Sieben on 16.03.2015.
 */
public class AllClassMembers {
    private static final HashMap<Class<?>, AllClassMembers> allMembersMap = new HashMap<>();
    public static final Map<String, Boolean> topPackages = new HashMap<>();

    public final ArrayList<Field> fields = new ArrayList<>();
    public final ArrayList<Method> methods = new ArrayList<>();
    public final Map<String, POJO> pojos = new TreeMap<>();
    public final ArrayList<BugProperty> properties = new ArrayList<>();

    public class POJO {
        public Method getter;
        public Method setter;
    }

    private AllClassMembers(Class<?> clazz) {
        addAllMembers(clazz, new HashSet<String>());
        Collections.sort(fields, new Comparator<Field>() { public int compare(Field o1, Field o2) { return o1.getName().compareTo(o2.getName());}});
        Collections.sort(methods, new Comparator<Method>() {@Override public int compare(Method o1, Method o2) {return o1.getName().compareTo(o2.getName());}});
        for (Field field: fields) properties.add(new BugFieldProperty(field));
        for (POJO pojo : pojos.values()) {
            properties.add(BugMethodProperty.getForPojo(pojo.getter, pojo.setter));
        }
    }

    private void addAllMembers(Class<?> clazz, Set<String> added) {
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        for (Method m : clazz.getDeclaredMethods()) {
            StringBuilder identifierSb = new StringBuilder().append(m.getReturnType().getName()).append(" ").append(m.getName());
            for (Class<?> p : m.getParameterTypes()) identifierSb.append(" ").append(p.getName());
            if (!added.add(identifierSb.toString())) continue;
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
        if (clazz.getSuperclass() != null) addAllMembers(clazz.getSuperclass(), added);
    }

    public static AllClassMembers getForClass(Class<?> clazz) {
        AllClassMembers allMembers = allMembersMap.get(clazz);
        if (allMembers == null)
            allMembersMap.put(clazz, allMembers = new AllClassMembers(clazz));
        return allMembers;
    }

    public static boolean topPackageExists(String name) {
        if (!name.endsWith(".")) name += ".";
        Boolean exists = topPackages.get(name);
        if (exists == null) {
            exists = false;
            for (Package p : Package.getPackages()) {
                if (p.getName().startsWith(name)) {
                    exists = true;
                    break;
                }
            }
            topPackages.put(name, exists);
        }

        return exists;
    }
}
