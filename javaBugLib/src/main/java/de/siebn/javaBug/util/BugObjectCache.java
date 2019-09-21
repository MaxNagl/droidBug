package de.siebn.javaBug.util;

import java.lang.ref.WeakReference;
import java.util.*;

public class BugObjectCache {
    private final static LinkedList<Object> keepFromGc = new LinkedList<>();
    private final static HashMap<String, HashMap<String, WeakReference<Object>>> references = new HashMap<>();

    public static String getReference(Object o) {
        if (o == null) return null;
        String clazz = o.getClass().getSimpleName();
        if (clazz.length() == 0) clazz = "object";
        String hash = Integer.toHexString(System.identityHashCode(o));
        HashMap<String, WeakReference<Object>> clazzRefs = references.get(clazz);
        if (clazzRefs == null) references.put(clazz, clazzRefs = new HashMap<>());
        WeakReference existing = clazzRefs.get(hash);
        if (existing == null) {
            clazzRefs.put(hash, new WeakReference<>(o));
        } else if (existing.get() != o) {
            int pf = 0;
            String postFix = null;
            while (existing != null && existing.get() != o) {
                postFix = Integer.toHexString(++pf);
                existing = clazzRefs.get(hash + postFix);
            }
            hash += postFix;
            if (existing != o) clazzRefs.put(hash, new WeakReference<>(o));
        }
        return clazz + "_" + hash;
    }

    public static Object get(String reference) {
        if (reference == null) return null;
        int uscroe = reference.indexOf('_');
        if (uscroe < 0) return null;
        String clazz = reference.substring(0, uscroe);
        if (clazz.length() == 0) clazz = "object";
        HashMap<String, WeakReference<Object>> clazzRefs = references.get(clazz);
        if (clazzRefs == null) return null;
        WeakReference<Object> ref = clazzRefs.get(reference.substring(uscroe + 1));
        if (ref == null) return null;
        Object o = ref.get();
        if (o == null) throw new IllegalArgumentException("Refrence \"" + reference + "\" was destroyed.");
        return o;
    }

    public static synchronized void keepFromGc(Object object) {
        keepFromGc.add(object);
    }

    public static synchronized void returnToGc(final Object object) {
        Iterator<Object> it = keepFromGc.iterator();
        while (it.hasNext()) {
            if (it.next() == object) {
                it.remove();
                return;
            }
        }
    }
}
