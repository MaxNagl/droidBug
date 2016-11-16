package de.siebn.javaBug.plugins;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.NanoHTTPD;
import de.siebn.javaBug.objectOut.OutputCategory;
import de.siebn.javaBug.objectOut.PropertyBuilder;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.XML;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Created by Sieben on 05.03.2015.
 */
public class ObjectBugPlugin implements RootBugPlugin.MainBugPlugin {
    public final HashMap<Integer, Object> references = new HashMap<>();
    private ArrayList<Object> rootObjects = new ArrayList<>();

    private JavaBug javaBug;

    public List<OutputCategory> getOutputCategories(Class<?> clazz) {
        ArrayList<OutputCategory> outputCategories = new ArrayList<>();
        for (OutputCategory oc : javaBug.getPlugins(OutputCategory.class))
            if (oc.canOutputClass(clazz))
                outputCategories.add(oc);
        return outputCategories;
    }

    public ObjectBugPlugin(JavaBug javaBug) {
        this.javaBug = javaBug;
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
    public int getOrder() {
        return 1000;
    }

    @JavaBug.Serve("^/objects/")
    public String serveObjects() {
        XML ul = new XML("ul");
        for (Object o : rootObjects) {
            PropertyBuilder builder = new PropertyBuilder();
            builder.createValue().setValue(o);
            builder.setExpandObject(this, o, o.getClass());
            builder.build(ul);
        }
        return ul.getXml();
    }

    @JavaBug.Serve("^/objectDetails/([^/]*)")
    public String serveObjectDetails(String[] params) {
        try {
            Object o = parseObjectReference(params[1]);
            return getObjectDetails(o, null);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    @JavaBug.Serve("^/objectDetails/([^/]*)/([^/]*)")
    public String serveObjectDetailsType(String[] params) {
        try {
            Object o = parseObjectReference(params[2]);
            return getObjectDetails(o, params[1]);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public String getObjectDetails(Object o, String type) {
        XML ul = new XML("ul");

        List<OutputCategory> outputCategories = javaBug.getObjectBug().getOutputCategories(o.getClass());
        for (OutputCategory oc : outputCategories) {
            if (oc.getType().equals(type)) {
                oc.add(ul, o);
                return ul.getXml();
            }
        }

        boolean alreadyOpened = false;
        for (OutputCategory oc : outputCategories) {
            String name = oc.getName(o);
            if (name != null) {
                PropertyBuilder builder = new PropertyBuilder();
                builder.setName(name);
                builder.setExpandLink(javaBug.getObjectBug().getObjectDetailsLink(o, oc.getType()));
                if (oc.opened(outputCategories, alreadyOpened)) {
                    XML expand = builder.createExtended("ul").setClass("expand");
                    oc.add(expand, o);
                    alreadyOpened = true;
                }
                XML ocul = builder.build(ul);
            }
        }
        return ul.getXml();
    }

    public static int parseHash(String hashString) {
        return Integer.parseInt(hashString.substring(hashString.startsWith("/") ? 2 : 1), 16);
    }

    public Object parseObjectReference(String reference) {
        return references.get(parseHash(reference));
    }

    public static String getHash(Object o) {
        return "@" + Integer.toHexString(System.identityHashCode(o));
    }

    public String getObjectReference(Object o) {
        references.put(System.identityHashCode(o), o);
        return getHash(o);
    }

    public String getObjectDetailsLink(Object o) {
        return "/objectDetails/" + getObjectReference(o);
    }

    public String getObjectDetailsLink(Object o, String type) {
        return "/objectDetails/" + type + "/" + getObjectReference(o);
    }

    public String getObjectEditLink(Object o, Field f) {
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        return "/objectEdit?object=" + getObjectReference(o) + "&field=" + allMembers.getFieldIdentifier(f);
    }

    @JavaBug.Serve("^/objectEdit")
    public String serveObjectEdit(NanoHTTPD.IHTTPSession session) throws Exception {
        Map<String, String> parms = session.getParms();
        Object o = parseObjectReference(parms.get("object"));
        String fieldName = parms.get("field");
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        Field f = allMembers.getField(fieldName);
        if (f != null) {
            TypeAdapters.TypeAdapter<?> adapter = TypeAdapters.getTypeAdapter(f.getType());
            if (adapter == null) throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "No TypeAdapter found!");
            Object val;
            String v = session.getParms().get("value");
            try {
                val = adapter.parse((Class) f.getType(), v == null ? null : v);
            } catch (Exception e) {
                throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Could not parse \"" + session.getParms().get("value") + "\"");
            }
            f.set(o, val);
            return TypeAdapters.toString(f.get(o));
        }
        return "ERROR";
    }

    public String getInvokationLink(boolean details, Object o, Method m, Object... predefined) {
        StringBuffer link = new StringBuffer("/invoke");
        link.append("?object=").append(getObjectReference(o));
        if (details) link.append("&details=").append(details ? "true" : "false");
        link.append("&method=").append(getHash(m));
        int param = 0;
        for (Object p : predefined) {
            if (p != null) link.append("&p").append(param).append("=").append(getObjectReference(p));
            param++;
        }
        return link.toString();
    }

    @JavaBug.Serve(value = "^/invoke", requiredParameters = {"object", "method"})
    public String serveInvoke(NanoHTTPD.IHTTPSession session) throws Exception {
        Map<String, String> parms = session.getParms();
        Object o = parseObjectReference(parms.get("object"));
        int methodHash = parseHash(parms.get("method"));
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        for (Method m : allMembers.methods) {
            if (System.identityHashCode(m) == methodHash) {
                Class<?>[] parameterTypes = m.getParameterTypes();
                Object[] ps = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> c = parameterTypes[i];
                    TypeAdapters.TypeAdapter adapter = TypeAdapters.getTypeAdapter(c);
                    String parameter = parms.get("p" + (i));
                    if (parameter != null) {
                        if (parameter.startsWith("@")) {
                            ps[i] = parseObjectReference(parameter);
                        } else {
                            ps[i] = adapter.parse(c, parameter);
                        }
                    } else if (parms.containsKey("p" + i)) {
                        ps[i] = parseObjectReference(parms.get("p" + i));
                    }
                }
                Object r = m.invoke(o, ps);
                if (r == null) return "null";
                if (parms.containsKey("details") && !parms.get("details").equals("false"))
                    return getObjectDetails(r, "string");
                else
                    return TypeAdapters.toString(r);
            }
        }
        throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Method not found");
    }

    public String getPojoLink(Object o, String field) {
        return "/pojo?object=" + getObjectReference(o) + "&field=" + field;
    }

    @JavaBug.Serve(value = "^/pojo", requiredParameters = {"object", "field"})
    public String servePojo(NanoHTTPD.IHTTPSession session) throws Exception {
        Map<String, String> parms = session.getParms();
        Object o = parseObjectReference(parms.get("object"));
        String fieldName = parms.get("field");
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        AllClassMembers.POJO pojo = allMembers.pojos.get(fieldName);
        if (pojo == null) throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Pojo not found!");
        if (session.getMethod() == NanoHTTPD.Method.GET) {
            if (pojo.getter == null) throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Getter found!");
            return TypeAdapters.toString(pojo.getter.invoke(o));
        }
        if (session.getMethod() == NanoHTTPD.Method.POST) {
            if (pojo.setter == null) throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "No setter found!");
            Method f = pojo.setter;
            Class type = f.getParameterTypes()[0];
            TypeAdapters.TypeAdapter<?> adapter = TypeAdapters.getTypeAdapter(type);
            if (adapter == null) throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "No TypeAdapter found!");
            Object val;
            String v = parms.get("value");
            try {
                val = adapter.parse(type, v == null ? null : v);
            } catch (Exception e) {
                throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Could not parse \"" + v + "\"");
            }
            f.invoke(o, val);
            if (pojo.getter != null)
                return TypeAdapters.toString(pojo.getter.invoke(o));
            return TypeAdapters.toString(val);
        }
        throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "ERROR");
    }
}
