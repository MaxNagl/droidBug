package de.siebn.javaBug.testApplication;

import java.util.LinkedHashMap;
import java.util.Map;

import de.siebn.javaBug.typeAdapter.TypeAdapters.AbstractTypeAdapter;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeSelectionAdapter;

public class ChooseIntAdapter extends AbstractTypeAdapter<Integer> implements TypeSelectionAdapter<Integer> {
    public ChooseIntAdapter() {
        super(10000, true);
    }

    @Override
    public String toString(Integer i) {
        return String.valueOf(i);
    }

    @Override
    public Integer parse(Class<? extends Integer> clazz, String string) {
        return Integer.parseInt(string);
    }

    @Override
    public Map<String, String> getValues(Class clazz) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("", "");
        map.put("1", "One");
        map.put("2", "Two");
        return map;
    }
}
