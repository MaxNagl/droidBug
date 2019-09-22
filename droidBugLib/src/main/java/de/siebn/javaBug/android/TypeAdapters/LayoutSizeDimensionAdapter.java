package de.siebn.javaBug.android.TypeAdapters;

import android.view.ViewGroup.LayoutParams;

import java.util.LinkedHashMap;
import java.util.Map;

import de.siebn.javaBug.typeAdapter.TypeAdapters.AbstractTypeAdapter;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeSelectionAdapter;

public class LayoutSizeDimensionAdapter extends AbstractTypeAdapter<Integer> implements TypeSelectionAdapter<Integer> {
    public LayoutSizeDimensionAdapter() {
        super(10, true);
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
        map.put(String.valueOf(LayoutParams.MATCH_PARENT), "MATCH_PARENT");
        map.put(String.valueOf(LayoutParams.WRAP_CONTENT), "WRAP_CONTENT");
        return map;
    }
}
