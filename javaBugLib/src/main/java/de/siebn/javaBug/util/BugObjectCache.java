package de.siebn.javaBug.util;

import java.util.HashMap;

public class BugObjectCache {
    private final static HashMap<String, HashMap<String, Object>> references = new HashMap<>();

    public static String getReference(Object o) {
        if (o == null) return null;
        String clazz = o.getClass().getSimpleName();
        String hash = Integer.toHexString(System.identityHashCode(o) % 3);
        HashMap<String, Object> clazzRefs = references.get(clazz);
        if (clazzRefs == null) references.put(clazz, clazzRefs = new HashMap<>());
        Object existing = clazzRefs.get(hash);
        if (existing == null) {
            clazzRefs.put(hash, o);
        } else if (existing != o) {
            int pf = 0;
            String postFix = null;
            while (existing != null && existing != o) {
                postFix = Integer.toHexString(++pf);
                existing = clazzRefs.get(hash + postFix);
            }
            hash += postFix;
            if (existing != o) clazzRefs.put(hash, o);
        }
        return clazz + "_" + hash;
    }

    public static Object get(String reference) {
        if (reference == null) return null;
        int uscroe = reference.indexOf('_');
        if (uscroe <= 0) return null;
        HashMap<String, Object> clazzRefs = references.get(reference.substring(0, uscroe));
        return clazzRefs == null ? null : clazzRefs.get(reference.substring(uscroe + 1));
    }
}
