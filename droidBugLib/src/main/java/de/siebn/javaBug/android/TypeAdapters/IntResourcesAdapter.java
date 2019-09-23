package de.siebn.javaBug.android.TypeAdapters;

import android.content.res.Resources;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import de.siebn.javaBug.android.BugResourcesUtil;
import de.siebn.javaBug.typeAdapter.TypeAdapters.*;
import de.siebn.javaBug.util.BugReflectionUtils;

public class IntResourcesAdapter extends AbstractTypeAdapter<Integer> implements TypeSelectionAdapter<Integer> {
    private List<StringPair> values = new ArrayList<>();
    private int addIndex = 1;

    @SuppressWarnings("ConstantConditions")
    public IntResourcesAdapter() {
        super(10, true);

        values.add(new StringPair("0", ""));
        Resources res = BugResourcesUtil.getResources();
        for (Field field : BugResourcesUtil.getAllResources().get("dimen")) {
            try {
                values.add(new StringPair(String.valueOf(res.getDimensionPixelSize((Integer) BugReflectionUtils.getOrNull(null, field))), field.getName()));
            } catch (Exception ignored) {
            }
        }
        for (Field field : BugResourcesUtil.getAllResources().get("integer")) {
            try {
                values.add(new StringPair(String.valueOf(res.getInteger((Integer) BugReflectionUtils.getOrNull(null, field))), field.getName()));
            } catch (Exception ignored) {
            }
        }
    }

    public IntResourcesAdapter addValue(String key, String value) {
        values.add(addIndex++, new StringPair(key, value));
        return this;
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
        return values;
    }
}
