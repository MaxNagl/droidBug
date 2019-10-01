package de.siebn.javaBug.plugins;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.JavaBugCore.BugEvaluator;
import de.siebn.javaBug.JavaBugCore.BugReferenceResolver;
import de.siebn.javaBug.objectOut.*;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;
import de.siebn.javaBug.util.*;

public class ObjectBugPlugin implements RootBugPlugin.MainBugPlugin, BugEvaluator, BugReferenceResolver {
    private HashMap<String, RootObject> rootObjects = new LinkedHashMap<>();

    public static class RootObject {
        public String name;
        public Object value;
    }

    private JavaBugCore javaBug;

    public ObjectBugPlugin(JavaBugCore javaBug) {
        this.javaBug = javaBug;
    }

    public List<BugOutputCategory> getOutputCategories(Class<?> clazz) {
        ArrayList<BugOutputCategory> outputCategories = new ArrayList<>();
        for (BugOutputCategory oc : javaBug.getPlugins(BugOutputCategory.class))
            if (oc.canOutputClass(clazz))
                outputCategories.add(oc);
        for (Method method : AllClassMembers.getForClass(clazz).methods) {
            BugOutputCategoryMethod output = method.getAnnotation(BugOutputCategoryMethod.class);
            if (output != null) {
                outputCategories.add(new BugGenericOutputCategory(output, clazz, method));
            }
        }
        Collections.sort(outputCategories, new Comparator<BugOutputCategory>() {
            @Override
            public int compare(BugOutputCategory o1, BugOutputCategory o2) {
                return o1.getOrder() - o2.getOrder();
            }
        });
        return outputCategories;
    }

    public void addRootObject(String name, Object value) {
        RootObject rootObject = new RootObject();
        rootObject.name = name;
        rootObject.value = value;
        rootObjects.put(name, rootObject);
    }

    public HashMap<String, RootObject> getRootObjects() {
        return rootObjects;
    }

    @Override
    public String getTabName() {
        return "Objects";
    }

    @Override
    public BugElement getContent() {
        return new BugInclude("/objects/");
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public static int visibleModifiers = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.STATIC | Modifier.VOLATILE | Modifier.INTERFACE | Modifier.VOLATILE;

    @JavaBugCore.Serve("^/objects/")
    public BugElement serveObjectsRoot(String[] params) {
        BugSplit split = new BugSplit(BugSplit.ORIENTATION_VERTICAL);
        BugList list = new BugList();
        list.addClazz("modFilter").format(BugFormat.tabContent).setId("ObjectBugList");
        for (RootObject o : rootObjects.values()) {
            list.add(getObjectElement(o.name, null, o.value));
        }
        split.add(new BugSplitElement(list));
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
        split.add(new BugSplitElement(options).setWeight("0").setFixed("auto").format(BugFormat.bgLight));
        return split;
    }

    @JavaBugCore.Serve("^/objects/([^/]*)/details/")
    public BugElement serveObjectsDetails(String[] params) {
        Object o = BugObjectCache.get(params[1]);
        BugList list = new BugList();
        boolean alreadyOpened = false;
        List<BugOutputCategory> outputCategories = getOutputCategories(o.getClass());
        for (BugOutputCategory cat : outputCategories) {
            String name = cat.getName(o);
            if (name != null) {
                BugEntry c = new BugEntry();
                c.elements.add(new BugText(name).setOnClick(BugText.ON_CLICK_EXPAND).format(BugFormat.category));
                String expandUrl = "/objects/" + BugObjectCache.getReference(o) + "/details/" + cat.getType();
                c.setExpandInclude(expandUrl);
                c.elements.add(BugText.NBSP);
                c.elements.add(BugInvokable.getExpandRefresh(expandUrl));
                if (cat.opened(outputCategories, alreadyOpened)) {
                    c.autoExpand = true;
                    alreadyOpened = true;
                }
                list.elements.add(c);
            }
        }
        return list;
    }

    @JavaBugCore.Serve("^/objects/([^/]*)/details/([^/]+)")
    public BugElement serveObjectsDetailsCategory(String[] params) {
        Object o = BugObjectCache.get(params[1]);
        String category = params[2];
        for (BugOutputCategory cat : getOutputCategories(o.getClass())) {
            if (cat.getType().equals(category)) {
                return cat.get(o);
            }
        }
        throw new IllegalArgumentException();
    }

    public BugElement getBugObjectFor(Object o) {
        if (o instanceof BugElement) return (BugElement) o;
        BugEntry e = new BugEntry();
        if (o != null) {
            e.elements.add(new BugText(o.getClass().getName()).format(BugFormat.clazz).setOnClick(BugText.ON_CLICK_EXPAND));
            e.elements.add(BugText.VALUE_SEPARATOR);
        }
        e.elements.add(BugText.getForValue(o));
        e.setExpandInclude("/objects/" + BugObjectCache.getReference(o) + "/details/");
        return e;
    }

    public static String getObjectDetailsLink(Object o) {
        if (o == null) return null;
        return "/objects/" + BugObjectCache.getReference(o) + "/details/";
    }

    public String getObjectDetailsLink(Object o, String type) {
        return "/objects/" + type + "/details/" + BugObjectCache.getReference(o);
    }

    public static String RETURN_TYPE_STRING = "string";
    public static String RETURN_TYPE_JSON = "json";

    @JavaBugCore.Serve(value = "^/invoke", requiredParameters = {"method"})
    public Object serveInvoke(NanoHTTPD.IHTTPSession session) throws Exception {
        Map<String, String> parms = session.getParms();
        Object o = BugObjectCache.get(parms.get("object"));
        Method m = (Method) BugObjectCache.get(parms.get("method"));
        Class<?>[] parameterTypes = m.getParameterTypes();
        Object[] ps = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> c = parameterTypes[i];
            TypeAdapter adapter = (TypeAdapter) BugObjectCache.get(parms.get("p" + i + "-adapter"));
            String parameter = parms.get("p" + i);
            String parameterType = parms.get("p" + i + "-type");
            String parameterClazz = parms.get("p" + i + "-clazz");
            if (parameterClazz != null) c = (Class<?>) BugObjectCache.get(parameterClazz);
            ps[i] = javaBug.eval(parameterType, parameter, c, adapter);
        }
        if (!m.isAccessible()) m.setAccessible(true);
        Object r = m.invoke(o, ps);
        String returnType = parms.get("returnType");
        if (RETURN_TYPE_STRING.equals(returnType)) {
            return TypeAdapters.toString(r);
        } else if (RETURN_TYPE_JSON.equals(returnType)) {
            return getBugObjectFor(r);
        } else {
            String rta = parms.get("adapter");
            TypeAdapters.TypeAdapter adapter = rta == null ? null : (TypeAdapter) BugObjectCache.get(rta);
            return TypeAdapters.toString(r, adapter);
        }
    }

    public BugElement getObjectElement(String title, String category, Object value) {
        BugEntry entry = new BugEntry();
        entry.setExpand(new BugInclude(javaBug.getObjectBug().getObjectDetailsLink(value)));
        if (title != null) {
            entry.add(new BugText(title).setOnClick(BugElement.ON_CLICK_EXPAND).format(BugFormat.title));
            entry.add(BugText.VALUE_SEPARATOR);
        }
        if (category != null) {
            entry.add(new BugText(category).setOnClick(BugElement.ON_CLICK_EXPAND).format(BugFormat.category));
            entry.add(BugText.VALUE_SEPARATOR);
        }
        entry.add(BugText.getForValueFormated(value).setOnClick(BugElement.ON_CLICK_EXPAND));
        return entry;
    }

    public static class InvocationLinkBuilder extends BugLinkBuilder {
        public InvocationLinkBuilder() {
            super("/invoke");
        }

        public InvocationLinkBuilder(Method m, Object o) {
            this();
            setMethod(m);
            if (!Modifier.isStatic(m.getModifiers())) setObject(o);
        }

        public InvocationLinkBuilder setObject(Object object) {
            return setParameter("object", BugObjectCache.getReference(object));
        }

        public InvocationLinkBuilder setMethod(Method method) {
            return setParameter("method", BugObjectCache.getReference(method));
        }

        public InvocationLinkBuilder setReturnType(String returnType) {
            return setParameter("returnType", returnType);
        }

        public InvocationLinkBuilder setPredefined(int param, Object value) {
            setParameter("p" + param, BugObjectCache.getReference(value));
            return setParameter("p" + param + "-type", "ref");
        }

        public InvocationLinkBuilder setPredefined(Object[] predefined) {
            if (predefined == null) return this;
            for (int i = 0; i < predefined.length; i++) setPredefined(i, predefined[i]);
            return this;
        }

        public InvocationLinkBuilder setTypeAdapter(int param, TypeAdapter<?> typeAdapter) {
            return setParameter("p" + param + "-adapter", BugObjectCache.getReference(typeAdapter));
        }

        public InvocationLinkBuilder setParameterClazz(int param, Class clazz) {
            return setParameter("p" + param + "-clazz", BugObjectCache.getReference(clazz));
        }

        public InvocationLinkBuilder setReturnTypeAdapter(TypeAdapter<?> typeAdapter) {
            return setParameter("adapter", BugObjectCache.getReference(typeAdapter));
        }

        public static InvocationLinkBuilder getGetter(Object o, Field f) {
            return new InvocationLinkBuilder(BugReflectionUtils.getOrNullMethod, null).setPredefined(0, o).setPredefined(1, f).setParameterClazz(2, f.getType());
        }

        public static InvocationLinkBuilder getSetter(Object o, Field f) {
            return new InvocationLinkBuilder(BugReflectionUtils.setMethod, null).setPredefined(0, o).setPredefined(1, f).setParameterClazz(2, f.getType());
        }
    }

    @Override
    public boolean canEvalType(String type) {
        if (type == null) return true;
        if ("null".equals(type)) return true;
        if ("text".equals(type)) return true;
        return "ref".equals(type);
    }

    @Override
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public Object eval(String type, String text, Class<?> clazz, TypeAdapter<?> adapter) {
        if ("null".equals(type)) return null;
        if ("ref".equals(type)) {
            Object value = BugObjectCache.get(text);
            if (value == null) throw new IllegalArgumentException("Refrence \"" + text + "\" not found.");
            return value;
        }
        if (adapter != null) return adapter.parse((Class) clazz, text);
        if (clazz != null) return TypeAdapters.getTypeAdapter(clazz).parse(clazz, text);
        throw new IllegalArgumentException("Either class of adapter must not me null.");
    }

    @Override
    public Object resolve(String reference) {
        if ("runOn".equals(reference) || "runon".equals(reference)) return BugThreadUtil.runOn;
        RootObject ro = rootObjects.get(reference);
        return ro == null ? null : ro.value;
    }

}
