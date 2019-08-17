package de.siebn.javaBug.android.TypeAdapters;

import android.content.res.Resources;

import de.siebn.javaBug.typeAdapter.TypeAdapters.AbstractTypeAdapter;

public class DpDimensionAdapter extends AbstractTypeAdapter<Integer> {
    public DpDimensionAdapter() {
        super(10, true);
    }

    @Override
    public String toString(Integer i) {
        if (i < 0) return "";
        return String.valueOf((int) (i / Resources.getSystem().getDisplayMetrics().density));
    }

    @Override
    public Integer parse(Class<? extends Integer> clazz, String string) {
        return (int) Math.round((Double.parseDouble(string) * Resources.getSystem().getDisplayMetrics().density));
    }

    @Override
    public String getUnit() {
        return "dp";
    }
}
