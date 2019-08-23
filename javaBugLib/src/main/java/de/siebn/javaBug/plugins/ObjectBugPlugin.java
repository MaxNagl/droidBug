package de.siebn.javaBug.plugins;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.objectOut.*;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;
import de.siebn.javaBug.util.*;

public class ObjectBugPlugin implements RootBugPlugin.MainBugPlugin {
    public final HashMap<Integer, Object> references = new HashMap<>();
    private ArrayList<RootObject> rootObjects = new ArrayList<>();

    public static class RootObject {
        String name;
        Object value;
    }

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

    public void addRootObject(String name, Object value) {
        RootObject rootObject = new RootObject();
        rootObject.name = name;
        rootObject.value = value;
        rootObjects.add(rootObject);
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
    public String getContentUrl() {
        return "/objectsJson/";
    }

    @Override
    public String getTagClass() {
        return "objects";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public static int visibleModifiers = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.STATIC | Modifier.VOLATILE | Modifier.INTERFACE;

    @JavaBug.Serve("^/objectsJson/")
    public BugElement serveObjectsJsonRoot(String[] params) {
        BugSplit split = new BugSplit(BugSplit.ORIENTATION_VERTICAL);
        BugList list = new BugList();
        list.addClazz("modFilter").format(BugFormat.paddingNormal);
        for (RootObject o : rootObjects) {
            list.add(getObjectElement(o.name, null, o.value));
        }
        split.add(new BugSplitElement(list.setId("ABCDEF")));
        BugEntry options = new BugEntry();
        for (Entry<Integer, String> mod : StringifierUtil.modifierNames.entrySet()) {
            BugInputCheckbox checkbox = new BugInputCheckbox(null, mod.getValue());
            checkbox.setOnChange("$('#" + list.id + "').toggleClass('show" + mod.getValue() + "');");
            checkbox.addClazz("checkWithBorder");
            if ((visibleModifiers & mod.getKey()) != 0) {
                checkbox.setChecked(true);
                list.addClazz("show" + mod.getValue());
            }
            options.add(checkbox).addSpace();
        }
        BugSplitElement e = new BugSplitElement(options);
        e.clazz = "colorBgLight";
        e.weight = "0";
        e.fixed = "20px";
        split.add(e);
        return split;
    }

    @JavaBug.Serve("^/objectsJson/([^/]*)/details/")
    public BugElement serveObjectsJsonDetails(String[] params) {
        Object o = parseObjectReference(params[1]);
        BugList list = new BugList();
        boolean alreadyOpened = false;
        List<OutputCategory> outputCategories = getOutputCategories(o.getClass());
        for (OutputCategory cat : outputCategories) {
            String name = cat.getName(o);
            if (name != null) {
                BugEntry c = new BugEntry();
                c.elements.add(new BugText(name).setOnClick(BugText.ON_CLICK_EXPAND).format(BugFormat.category));
                c.expand = "/objectsJson/" + getObjectReference(o) + "/details/" + cat.getId();
                c.elements.add(BugText.NBSP);
                c.elements.add(BugInvokable.getExpandRefresh((String) c.expand));
                if (cat.opened(outputCategories, alreadyOpened)) {
                    c.autoExpand = true;
                    alreadyOpened = true;
                }
                list.elements.add(c);
            }
        }
        return list;
    }

    @JavaBug.Serve("^/objectsJson/([^/]*)/details/([^/]+)")
    public BugElement serveObjectsJsonDetailsCategory(String[] params) {
        Object o = parseObjectReference(params[1]);
        String category = params[2];
        for (OutputCategory cat : getOutputCategories(o.getClass())) {
            if (cat.getId().equals(category)) {
                return cat.get(o);
            }
        }
        throw new IllegalArgumentException();
    }

    public BugElement getJsonBugObjectFor(Object o) {
        BugEntry e = new BugEntry();
        e.elements.add(new BugText(o.getClass().getName()).format(BugFormat.clazz).setOnClick(BugText.ON_CLICK_EXPAND));
        e.elements.add(BugText.VALUE_SEPARATOR);
        e.elements.add(BugText.getForValue(o).format(BugFormat.value));
        e.expand = "/objectsJson/" + getObjectReference(o) + "/details/";
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

    public String getObjectDetailsLinkJson(Object o) {
        if (o == null) return null;
        return "/objectsJson/" + getObjectReference(o) + "/details/";
    }

    public String getObjectDetailsLinkJson(Object o, String type) {
        return "/objectsJson/" + type + "/details/" + getObjectReference(o);
    }

    public String getObjectGetLink(Object o, Field f) {
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        return "/objectGet?object=" + getObjectReference(o) + "&field=" + allMembers.getFieldIdentifier(f);
    }

    public String getObjectEditLink(Object o, Field f) {
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        return "/objectEdit?object=" + getObjectReference(o) + "&field=" + allMembers.getFieldIdentifier(f);
    }

    @JavaBug.Serve("^/objectGet")
    public String serveObjectGet(NanoHTTPD.IHTTPSession session) throws Exception {
        Map<String, String> parms = session.getParms();
        Object o = parseObjectReference(parms.get("object"));
        String fieldName = parms.get("field");
        AllClassMembers allMembers = AllClassMembers.getForClass(o.getClass());
        Field f = allMembers.getField(fieldName);
        if (f != null) {
            return TypeAdapters.toString(f.get(o));
        }
        throw new IllegalArgumentException();
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

    public BugElement getObjectElement(String title, String category, Object value) {
        BugEntry entry = new BugEntry();
        entry.setExpand(javaBug.getObjectBug().getObjectDetailsLinkJson(value));
        if (title != null) {
            entry.add(new BugText(title).setOnClick(BugElement.ON_CLICK_EXPAND).format(BugFormat.title));
            entry.add(BugText.VALUE_SEPARATOR);
        }
        if (category != null) {
            entry.add(new BugText(category).setOnClick(BugElement.ON_CLICK_EXPAND).format(BugFormat.category));
            entry.add(BugText.VALUE_SEPARATOR);
        }
        entry.add(BugText.getForValue(value).setOnClick(BugElement.ON_CLICK_EXPAND));
        return entry;
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
