package de.siebn.javaBug.plugins;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.JsonBugBase;
import de.siebn.javaBug.JsonBugList;
import de.siebn.javaBug.JsonBugEntry;
import de.siebn.javaBug.NanoHTTPD;
import de.siebn.javaBug.objectOut.AnnotatedOutputCategory;
import de.siebn.javaBug.objectOut.ListItemBuilder;
import de.siebn.javaBug.objectOut.OutputCategory;
import de.siebn.javaBug.objectOut.OutputMethod;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;
import de.siebn.javaBug.util.AllClassMembers;
import de.siebn.javaBug.util.XML;

public class ObjectBugPlugin implements RootBugPlugin.MainBugPlugin {
    public final HashMap<Integer, Object> references = new HashMap<>();
    private ArrayList<Object> rootObjects = new ArrayList<>();

    private JavaBug javaBug;

    public ObjectBugPlugin(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    public static int parseHash(String hashString) {
        return Integer.parseInt(hashString.substring(hashString.startsWith("/") ? 2 : 1), 16);
    }

    public static String getHash(Object o) {
        return "@" + Integer.toHexString(System.identityHashCode(o));
    }

    public List<OutputCategory> getOutputCategories(Class<?> clazz) {
        ArrayList<OutputCategory> outputCategories = new ArrayList<>();
        for (OutputCategory oc : javaBug.getPlugins(OutputCategory.class))
            if (oc.canOutputClass(clazz))
                outputCategories.add(oc);
        for (Method method : AllClassMembers.getForClass(clazz).methods) {
            OutputMethod output = method.getAnnotation(OutputMethod.class);
            if (output != null) {
                outputCategories.add(new AnnotatedOutputCategory(output, clazz, method));
            }
        }
        Collections.sort(outputCategories, new Comparator<OutputCategory>() {
            @Override
            public int compare(OutputCategory o1, OutputCategory o2) {
                return o1.getOrder() - o2.getOrder();
            }
        });
        return outputCategories;
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

    @JavaBug.Serve("^/objectsJson/")
    public JsonBugBase serveObjectsJsonRoot(String[] params) {
        JsonBugList list = new JsonBugList();
        for (Object o : rootObjects) {
            list.elements.add(getJsonBugObjectFor(o));
        }
        return list;
    }

    @JavaBug.Serve("^/objectsJson/([^/]*)/details/")
    public JsonBugBase serveObjectsJsonDetails(String[] params) {
        Object o = parseObjectReference(params[1]);
        JsonBugList list = new JsonBugList();
        for (OutputCategory cat : getOutputCategories(o.getClass())) {
            JsonBugEntry c = new JsonBugEntry();
            c.name = cat.getName(o);
            c.expand = "/objectsJson/" + getObjectReference(o) + "/details/" + cat.getId();
            list.elements.add(c);
        }
        return list;
    }

    @JavaBug.Serve("^/objectsJson/([^/]*)/details/([^/]+)")
    public JsonBugBase serveObjectsJsonDetailsCategory(String[] params) {
        Object o = parseObjectReference(params[1]);
        String category = params[2];
        JsonBugList list = new JsonBugList();
        for (OutputCategory cat : getOutputCategories(o.getClass())) {
            if (cat.getId().equals(category)) {
                cat.add(list, o);
            }
        }
        return list;
    }

    public JsonBugEntry getJsonBugObjectFor(Object o) {
        JsonBugEntry e = new JsonBugEntry();
        e.name = o.getClass().getName();
        e.expand = "/objectsJson/" + getObjectReference(o) + "/details/";
        e.value = TypeAdapters.toString(o);
        return e;
    }

    @JavaBug.Serve("^/objects/")
    public String serveObjects() {
        XML ul = new XML("ul");
        for (Object o : rootObjects) {
            ListItemBuilder builder = new ListItemBuilder();
            builder.addValue().setValue(o);
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
                ListItemBuilder builder = new ListItemBuilder();
                builder.setName(name);
                builder.setExpandLink(javaBug.getObjectBug().getObjectDetailsLink(o, oc.getType()));
                builder.setRefreshLink(javaBug.getObjectBug().getObjectDetailsLink(o, oc.getType()));
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

    public Object parseObjectReference(String reference) {
        return references.get(parseHash(reference));
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
            if (adapter == null)
                throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "No TypeAdapter found!");
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

    public static String RETURN_TYPE_STRING = "string";
    public static String RETURN_TYPE_XML = "xml";
    public static String RETURN_TYPE_JSON = "json";

    public String getInvokationLink(String returnType, Object o, Method m, Object... predefined) {
        StringBuilder link = new StringBuilder("/invoke");
        link.append("?object=").append(getObjectReference(o));
        link.append("&returnType=").append(returnType);
        link.append("&method=").append(getHash(m));
        int param = 0;
        for (Object p : predefined) {
            if (p != null)
                link.append("&p").append(param).append("=").append(getObjectReference(p));
            param++;
        }
        return link.toString();
    }

    @JavaBug.Serve(value = "^/invoke", requiredParameters = {"object", "method"})
    public Object serveInvoke(NanoHTTPD.IHTTPSession session) throws Exception {
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
                    TypeAdapters.TypeAdapter adapter;
                    String ta = parms.get("ta" + (i));
                    if (ta != null) {
                        adapter = TypeAdapters.getTypeAdapterClass((Class<?>) parseObjectReference(ta));
                    } else {
                        adapter = TypeAdapters.getTypeAdapter(c);
                    }
                    String parameter = parms.get("p" + i);
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
                String returnType = parms.get("returnType");
                if (RETURN_TYPE_STRING.equals(returnType)) {
                    return getObjectDetails(r, "string");
                } else if (RETURN_TYPE_JSON.equals(returnType)) {
                    return getJsonBugObjectFor(r);
                } else {
                    String rta = parms.get("rta");
                    TypeAdapters.TypeAdapter adapter = rta == null ? null : TypeAdapters.getTypeAdapterClass((Class<?>) parseObjectReference(rta));
                    return TypeAdapters.toString(r, adapter);
                }
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
        if (pojo == null)
            throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Pojo not found!");
        if (session.getMethod() == NanoHTTPD.Method.GET) {
            if (pojo.getter == null)
                throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "Getter found!");
            return TypeAdapters.toString(pojo.getter.invoke(o));
        }
        if (session.getMethod() == NanoHTTPD.Method.POST) {
            if (pojo.setter == null)
                throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "No setter found!");
            Method f = pojo.setter;
            Class type = f.getParameterTypes()[0];
            TypeAdapters.TypeAdapter<?> adapter = TypeAdapters.getTypeAdapter(type);
            if (adapter == null)
                throw new JavaBug.ExceptionResult(NanoHTTPD.Response.Status.BAD_REQUEST, "No TypeAdapter found!");
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

    public class InvocationLinkBuilder {
        private Object object;
        private boolean details;
        private Method method;
        private HashMap<Integer, Object> predefined;
        private HashMap<Integer, Class<?>> typeAdapters;
        private Class<?> returnTypeAdapter;

        public InvocationLinkBuilder setObject(Object object) {
            this.object = object;
            return this;
        }

        public InvocationLinkBuilder setDetails(boolean details) {
            this.details = details;
            return this;
        }

        public InvocationLinkBuilder setMethod(Method method) {
            this.method = method;
            return this;
        }

        public InvocationLinkBuilder setPredefined(int param, Object value) {
            if (this.predefined == null) this.predefined = new HashMap<>();
            this.predefined.put(param, value);
            return this;
        }

        public InvocationLinkBuilder setPredefinedList(Object... predefined) {
            for (int i = 0; i < predefined.length; i++) {
                if (predefined[i] != null) setPredefined(i, predefined[i]);
            }
            return this;
        }

        public InvocationLinkBuilder setTypeAdapter(int param, TypeAdapter<?> typeAdapter) {
            if (this.typeAdapters == null) this.typeAdapters = new HashMap<>();
            this.typeAdapters.put(param, typeAdapter.getClass());
            return this;
        }

        public InvocationLinkBuilder setReturTypeAdapter(TypeAdapter<?> typeAdapter) {
            this.returnTypeAdapter = typeAdapter.getClass();
            return this;
        }

        public String build() {
            StringBuilder link = new StringBuilder("/invoke");
            link.append("?object=").append(getObjectReference(object));
            if (details) link.append("&details=true");
            link.append("&method=").append(getHash(method));
            if (predefined != null) {
                for (Entry<Integer, Object> entry : predefined.entrySet()) {
                    link.append("&p").append(entry.getKey()).append("=").append(getObjectReference(entry.getValue()));
                }
            }
            if (typeAdapters != null) {
                for (Entry<Integer, Class<?>> entry : typeAdapters.entrySet()) {
                    link.append("&ta").append(entry.getKey()).append("=").append(getObjectReference(entry.getValue()));
                }
            }
            if (returnTypeAdapter != null) {
                link.append("&rta=" + getObjectReference(returnTypeAdapter));
            }
            return link.toString();
        }
    }
}
