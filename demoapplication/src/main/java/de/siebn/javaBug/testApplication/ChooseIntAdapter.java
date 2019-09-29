package de.siebn.javaBug.testApplication;

import java.util.*;

import de.siebn.javaBug.typeAdapter.TypeAdapters.*;

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
    public List<StringPair> getValues(Class clazz) {
        List<StringPair> map = new ArrayList<>();
        map.add(new StringPair("", ""));
        map.add(new StringPair("1", "One"));
        map.add(new StringPair("2", "Two"));
        return map;
    }
}
