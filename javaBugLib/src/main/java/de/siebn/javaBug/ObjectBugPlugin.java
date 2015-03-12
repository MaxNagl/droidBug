package de.siebn.javaBug;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * Created by Sieben on 05.03.2015.
 */
public class ObjectBugPlugin implements RootBugPlugin.MainBugPlugin {
    public final static ObjectBugPlugin INSTANCE = new ObjectBugPlugin();

    private final HashMap<Class<?>, AllClassMembers> allMembersMap = new HashMap<>();
    private final WeakHashMap<Integer, Object> references = new WeakHashMap<>();
    private ArrayList<Object> rootObjects = new ArrayList<>();

    private ObjectBugPlugin() {
    }

    public void addRootObject(Object object) {
        rootObjects.add(object);
    }

    @Override
    public String getTabName() {
        return "Objects";
    }

    @Override
    public String getUrl() {
        return "/objects/";
    }

    @Override
    public String getTagClass() {
        return "objects";
    }

    @Override
    public int getPriority() {
        return 1000;
    }

    private class AllClassMembers {
        private ArrayList<Field> fields = new ArrayList<>();
        private ArrayList<Method> methods = new ArrayList<>();
        private AllClassMembers(Class<?> clazz) {
            addAllMembers(clazz);
            Collections.sort(fields, new Comparator<Field>() {
                @Override
                public int compare(Field o1, Field o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            Collections.sort(methods, new Comparator<Method>() {@Override public int compare(Method o1, Method o2) {return o1.getName().compareTo(o2.getName());}});
        }

        private void addAllMembers(Class<?> clazz) {
            Collections.addAll(fields, clazz.getDeclaredFields());
            Collections.addAll(methods, clazz.getDeclaredMethods());
            if (clazz.getSuperclass() != null) addAllMembers(clazz.getSuperclass());
        }
    }

    @JavaBug.Serve("^/objects/")
    public String serveObjects() {
        XML ul = new XML("ul");
        for (Object o : rootObjects) {
            addObjectInfo(ul.add("li").setClass("object"), o, null, null);
        }
        return ul.getXml();
    }

    private AllClassMembers getAllMembers(Class<?> clazz) {
        AllClassMembers allMembers = allMembersMap.get(clazz);
        if (allMembers == null)
            allMembersMap.put(clazz, allMembers = new AllClassMembers(clazz));
        return allMembers;
    }

    @JavaBug.Serve("^/objectDetails/([^/]*)")
    public String serveObjectDetails(String[] params) {
        try {
            int hash = Integer.parseInt(params[1], 16);
            Object o = references.get(hash);
            return getObjectDetails(o, false);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    private String getObjectDetails(Object o, boolean canShowOverview) throws IllegalAccessException {
        if (canShowOverview) {
            TypeAdapters.TypeAdapter adapter = TypeAdapters.getTypeAdapter(o.getClass());
            if (adapter.showOverview()) {
                XML ul = new XML("ul");
                XML li = ul.add("li").setClass("object");
                li.setAttr("expand", getObjectDetailsLink(o));
                li.add("span").setClass("type").appendText(o.getClass().getSimpleName());
                li.appendText(" ").add("span").setClass("value").appendText(TypeAdapters.toString(o));
                return ul.getXml();
            }
        }
        String hexHash = Integer.toHexString(System.identityHashCode(o));
        AllClassMembers allMembers = getAllMembers(o.getClass());
        XML ul = new XML("ul");
        for (Field f : allMembers.fields) {
            f.setAccessible(true);
            XML li = ul.add("li").setClass("object");
            addModifiers(li, f.getModifiers());
            li.add("span").setClass("type").appendText(f.getType().getSimpleName());
            li.add("span").appendText(" ").setClass("fieldName").appendText(f.getName());
            li.add("span").setClass("equals").appendText(" = ");
            addObjectInfo(li, f.get(o), hexHash, f);
        }
        for (Method m : allMembers.methods) {
            m.setAccessible(true);
            XML li = ul.add("li").setClass("object notOpenable");
            addModifiers(li, m.getModifiers());
            li.add("span").setClass("type").appendText(m.getReturnType().getSimpleName());
            li.appendText(" ").add("span").setClass("fieldName").appendText(m.getName());
            li.appendText("(");
            boolean first = true;
            for (Class c : m.getParameterTypes()) {
                if (!first) li.appendText(", ");
                li.add("span").setClass("type").appendText(c.getSimpleName());
                first = false;
            }
            li.appendText(")");
            if (m.getParameterTypes().length == 0) {
                references.put(System.identityHashCode(o), o);
                li.setAttr("invoke", "/invoke/" + hexHash + "/" + m.getName());
            }
        }
        return ul.getXml();
    }

    private void addModifiers(XML tag, int modifiers) {
        if (modifiers != 0) {
            tag.add("span").setClass("modifier").appendText(Modifier.toString(modifiers));
            tag.appendText(" ");
        }
    }

    public void addObjectInfo(XML li, Object o, String parentHash, Field field) {
        if (o != null && (field == null || !field.getType().isPrimitive())) {
            li.setAttr("expand", getObjectDetailsLink(o));
        } else {
            li.addClass("notOpenable");
        }
        XML f = li.add("span").setClass("field");
        XML v = f.add("span").setClass("value");
        TypeAdapters.TypeAdapter<Object> adapter = TypeAdapters.getTypeAdapter(o == null ? Object.class : o.getClass());
        if (o != null && parentHash != null && field != null && !Modifier.isFinal(field.getModifiers()) && adapter.canParse(o.getClass())) {
            v.setAttr("editurl", "/objectEdit/" + parentHash + "/" + field.getName());
            if (!field.getType().isPrimitive())
                v.setAttr("editNullify", "true");
        }
        v.appendText(TypeAdapters.toString(o));
    }

    public String getObjectDetailsLink(Object o) {
        references.put(System.identityHashCode(o), o);
        String hexHash = Integer.toHexString(System.identityHashCode(o));
        return "/objectDetails/" + hexHash;
    }

    @JavaBug.Serve("^/objectEdit/([^/]*)/([^/]*)")
    public String serveObjectEdit(NanoHTTPD.IHTTPSession session, String[] params) throws Exception {
        if (session.getMethod() == NanoHTTPD.Method.POST)
            session.parseBody(null);
        int hash = Integer.parseInt(params[1], 16);
        String fieldName = params[2];
        Object o = references.get(hash);
        AllClassMembers allMembers = getAllMembers(o.getClass());
        for (Field f : allMembers.fields) {
            if (f.getName().equals(fieldName)) {
                TypeAdapters.TypeAdapter<?> adapter = TypeAdapters.getTypeAdapter(f.getType());
                if (adapter == null) throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "No TypeAdapter found!");
                Object val;
                try {
                    String v = session.getParms().get("o");
                    val = adapter.parse((Class) f.getType(), v == null ? null : v);
                } catch (Exception e) {
                    throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Could not parse + \"" + session.getParms().get("o") + "\"");
                }
                f.set(o, val);
                return String.valueOf(f.get(o));
            }
        }
        return "ERROR";
    }

    @JavaBug.Serve("^/invoke/([^/]*)/([^/]*)")
    public String serveInvoke(NanoHTTPD.IHTTPSession session, String[] params) throws Exception {
        int hash = Integer.parseInt(params[1], 16);
        String methodName = params[2];
        Object o = references.get(hash);
        AllClassMembers allMembers = getAllMembers(o.getClass());
        for (Method m : allMembers.methods) {
            if (m.getName().equals(methodName)) {
                return getObjectDetails(m.invoke(o), true);
            }
        }
        return "ERROR";
    }
}
