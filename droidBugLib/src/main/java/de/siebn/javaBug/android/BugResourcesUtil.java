package de.siebn.javaBug.android;

import android.content.res.Resources;

import java.lang.reflect.Field;
import java.util.*;

public class BugResourcesUtil {
    private final static List<Class<?>> Rs = new ArrayList<>();
    private final static Map<String, List<Field>> allResources = new HashMap<>();
    private static Resources resources;

    public static void addPackage(String packageName) {
        try {
            Class<?> r  = Class.forName(packageName + ".R");
            for (Class<?> c : r.getClasses()) {
                List<Field> list = allResources.get(c.getSimpleName());
                if (list == null) allResources.put(c.getSimpleName(), list = new ArrayList<>());
                list.addAll(Arrays.asList(c.getFields()));
            }
        } catch (Exception ignored) {
        }
    }

    public static Map<String, List<Field>> getAllResources() {
        return allResources;
    }

    public static void setResources(Resources resources) {
        BugResourcesUtil.resources = resources;
    }

    public static Resources getResources() {
        return resources;
    }
}
