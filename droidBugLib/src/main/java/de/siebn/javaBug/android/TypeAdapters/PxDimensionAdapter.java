package de.siebn.javaBug.android.TypeAdapters;

import de.siebn.javaBug.typeAdapter.TypeAdapters.AbstractTypeAdapter;

public class PxDimensionAdapter extends AbstractTypeAdapter<Integer> {
    public PxDimensionAdapter() {
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
    public String getUnit() {
        return "px";
    }
}
