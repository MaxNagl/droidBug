package de.siebn.javaBug.testApplication;

import de.siebn.javaBug.typeAdapter.TypeAdapters.AbstractTypeAdapter;

public class DoubleIntAdapter extends AbstractTypeAdapter<Integer> {
    public DoubleIntAdapter() {
        super(10000, true);
    }

    @Override
    public String toString(Integer i) {
        return String.valueOf((long) i * 2);
    }

    @Override
    public Integer parse(Class<? extends Integer> clazz, String string) {
        return (int) (Double.parseDouble(string) / 2);
    }

    @Override
    public String getUnit() {
        return "x2";
    }
}
