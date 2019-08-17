package de.siebn.javaBug.typeAdapter;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeAdapters {
    private final static ArrayList<TypeAdapter<?>> adapters = new ArrayList<>();
    private final static Map<Class<?>, TypeAdapter> adapterMap = new HashMap<>();

    static {
        addAdapter(new PrimitiveAdapter());
        addAdapter(new ObjectAdapter());
        addAdapter(new ArrayAdapter());
        addAdapter(new StringAdapter());
    }


    private TypeAdapters() {}

    @SuppressWarnings("unchecked")
    public static<T> TypeAdapter<T> getTypeAdapter(Class clazz) {
        TypeAdapter<?> adapter = adapterMap.get(clazz);
        if (adapter != null) return (TypeAdapter<T>) adapter;
        for (TypeAdapter<?> t : adapters) {
            if (t.canAdapt(clazz)) {
                adapterMap.put(clazz, t);
                return (TypeAdapter<T>) t;
            }
        }
        adapterMap.put(clazz, null);
        return null;
    }

    @SuppressWarnings("unchecked")
    public static<T> TypeAdapter<T> getTypeAdapterClass(Class<?> clazz) {
        for (TypeAdapter t : adapters) {
            if (t.getClass().equals(clazz)) {
                return t;
            }
        }
        try {
            return (TypeAdapter<T>) clazz.getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static<T> List<TypeAdapter<T>> getTypeAdapterClasses(Class[] classes) {
        ArrayList<TypeAdapter<T>> list = new ArrayList<>();
        for (Class<?> clazz : classes) {
            list.add((TypeAdapter<T>) getTypeAdapterClass(clazz));
        }
        return list;
    }

    public static boolean canParse(Class clazz) {
        TypeAdapter adapter = getTypeAdapter(clazz);
        return adapter.canParse(clazz);
    }

    public static String toString(Object o) {
        return toString(o, null, 100);
    }

    public static String toString(Object o, TypeAdapter adapter) {
        return toString(o, adapter, 100);
    }

    @SuppressWarnings("ConstantConditions")
    public static String toString(Object o, TypeAdapter adapter, int maxLength) {
        if (o == null) return "null";
        if (adapter == null) adapter = TypeAdapters.getTypeAdapter(o.getClass());
        String s = adapter.toString(o);
        if (s.length() > maxLength) s = s.substring(0, maxLength - 3) + "...";
        return s;
    }

    public static void addAdapter(TypeAdapter adapter) {
        adapters.add(adapter);
        Collections.sort(adapters, new Comparator<TypeAdapter<?>>() {
            @Override
            public int compare(TypeAdapter<?> o1, TypeAdapter<?> o2) {
                return o2.getProirity() - o1.getProirity();
            }
        });
        adapterMap.clear();
    }

    public interface TypeAdapter<T> {
        T parse(Class<? extends T> clazz, String string);
        String toString(T object);
        boolean canAdapt(Class<?> clazz);
        boolean canParse(Class<?> clazz);
        int getProirity(); // Adapters with a high priority are prefered
        String getUnit();
    }

    public interface TypeSelectionAdapter<T> extends TypeAdapter<T> {
        Map<String, String> getValues();
    }

    public static abstract class AbstractTypeAdapter<T> implements TypeAdapter<T> {
        protected final Set<Class<? extends T>> classes = new HashSet<>();
        protected final int proirity;
        private final boolean canParse;

        public AbstractTypeAdapter(int proirity, boolean canParse) {
            this.proirity = proirity;
            this.canParse = canParse;
        }

        @Override
        public int getProirity() {
            return proirity;
        }

        @Override
        public boolean canAdapt(Class<?> clazz) {
            return classes.contains(clazz);
        }

        @Override
        public String toString(T object) {
            return String.valueOf(object);
        }

        @Override
        public boolean canParse(Class<?> clazz) {
            return canParse;
        }

        @Override
        public T parse(Class<? extends T> clazz, String string) {
            throw new IllegalStateException();
        }

        @Override
        public String getUnit() {
            return null;
        }
    }

    public static class PrimitiveAdapter extends AbstractTypeAdapter<Object> {

        public PrimitiveAdapter() {
            super(Integer.MIN_VALUE + 2, true);
            classes.add(Integer.class);
            classes.add(Long.class);
            classes.add(Boolean.class);
            classes.add(Float.class);
            classes.add(Double.class);
            classes.add(Byte.class);
            classes.add(Short.class);
            classes.add(Character.class);
        }

        @Override
        public Object parse(Class<?> clazz, String string) {
            if (clazz.equals(Integer.class) || clazz.equals(int.class)) return Integer.parseInt(string);
            if (clazz.equals(Long.class) || clazz.equals(long.class)) return Long.parseLong(string);
            if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) return Boolean.parseBoolean(string);
            if (clazz.equals(Float.class) || clazz.equals(float.class)) return Float.parseFloat(string);
            if (clazz.equals(Double.class) || clazz.equals(double.class)) return Double.parseDouble(string);
            if (clazz.equals(Byte.class) || clazz.equals(byte.class)) return Byte.parseByte(string);
            if (clazz.equals(Short.class) || clazz.equals(short.class)) return Short.parseShort(string);
            if (clazz.equals(Character.class) || clazz.equals(char.class)) return string.charAt(0);
            return null;
        }

        @Override
        public boolean canAdapt(Class<?> clazz) {
            return clazz.isPrimitive() || super.canAdapt(clazz);
        }
    }

    public static class StringAdapter extends AbstractTypeAdapter<Object> {

        public StringAdapter() {
            super(Integer.MIN_VALUE + 2, true);
            classes.add(String.class);
            classes.add(CharSequence.class);
        }

        @Override
        public Object parse(Class<?> clazz, String string) {
            return string;
        }
    }

    public static class ArrayAdapter extends AbstractTypeAdapter<Object> {
        public ArrayAdapter() {
            super(Integer.MIN_VALUE + 1, false);
        }

        @Override
        public boolean canAdapt(Class<?> clazz) {
            return clazz.isArray();
        }

        @Override
        public String toString(Object object) {
            return object.getClass().getComponentType().getSimpleName() + "[" + Array.getLength(object) + "]";
        }
    }

    public static class ObjectAdapter extends AbstractTypeAdapter<Object> {
        public ObjectAdapter() {
            super(Integer.MIN_VALUE, false);
        }

        @Override
        public boolean canAdapt(Class<?> clazz) {
            return true;
        }

        @Override
        public String toString(Object object) {
            if (object == null) return "null";
            try {
                if (object.getClass().getMethod("toString").equals(Object.class.getMethod("toString")))
                    return object.getClass().getName();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            return super.toString(object);
        }
    }
}
