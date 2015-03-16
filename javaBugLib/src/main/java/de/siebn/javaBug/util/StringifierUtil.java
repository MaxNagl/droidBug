package de.siebn.javaBug.util;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Sieben on 13.03.2015.
 */
public class StringifierUtil {
    public final static String[] timeUnits = {"ns", "µs", "ms", "s"};
    public final static Map<Integer, String> modifierNames = getModifierMap();

    public static String nanoSecondsToString(long ns) {
        float d = ns;
        int unit = 0;
        while (unit < timeUnits.length && d > 1000) {
            d /= 1000;
            unit++;
        }
        return (Math.round(d * 10) / 10f) + timeUnits[unit];
    }

    public static String modifiersToString(int modifiers, String prefix, boolean includePackage) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> e : modifierNames.entrySet()) {
            boolean match = (modifiers & e.getKey()) != 0;
            if (includePackage && e.getKey() == 0)
                match = (modifiers & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) == 0;
            if (match) {
                if (sb.length() != 0) sb.append(" ");
                if (prefix != null) sb.append(prefix);
                sb.append(e.getValue());
            }
        }
        return sb.toString();
    }

    private static Map<Integer, String> getModifierMap() {
        LinkedHashMap map = new LinkedHashMap();

        map.put(Modifier.PUBLIC, "public");
        map.put(0, "package");
        map.put(Modifier.PRIVATE, "private");
        map.put(Modifier.PROTECTED, "protected");
        map.put(Modifier.STATIC, "static");
        map.put(Modifier.FINAL, "final");
        map.put(Modifier.SYNCHRONIZED, "synchronized");
        map.put(Modifier.VOLATILE, "volatile");
        map.put(Modifier.TRANSIENT, "transient");
        map.put(Modifier.NATIVE, "native");
        map.put(Modifier.INTERFACE, "interface");
        map.put(Modifier.ABSTRACT, "abstract");
        map.put(Modifier.STRICT, "strict");

        return Collections.unmodifiableMap(map);
    }
}
