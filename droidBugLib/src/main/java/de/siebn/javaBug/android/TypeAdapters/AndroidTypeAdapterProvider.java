package de.siebn.javaBug.android.TypeAdapters;

import android.view.ViewDebug.ExportedProperty;
import android.view.ViewDebug.IntToString;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapterProvider;

public class AndroidTypeAdapterProvider extends TypeAdapterProvider {
    private final HashMap<ExportedProperty, IntResourcesAdapter> exportedIntPropertyCache = new HashMap<>();

    private final PxDimensionAdapter pxDimensionAdapter = new PxDimensionAdapter();
    private final DpDimensionAdapter dpDimensionAdapter = new DpDimensionAdapter();

    @Override
    public void getForField(List<TypeAdapter> list, Field field) {
        Class<?> type = field.getType();
        if (type == int.class || type == Integer.class) {
            list.add(pxDimensionAdapter);
            list.add(dpDimensionAdapter);
            list.add(getForExportedProperty(field.getAnnotation(ExportedProperty.class)));
        }
    }

    public TypeAdapter getForExportedProperty(ExportedProperty exportedProperty) {
        IntResourcesAdapter ra = exportedIntPropertyCache.get(exportedProperty);
        if (ra == null) {
            ra = new IntResourcesAdapter();
            IntToString[] mapping = exportedProperty == null ? null : exportedProperty.mapping();
            if (exportedProperty != null) for (IntToString m : mapping) {
                ra.addValue(String.valueOf(m.from()), m.to());
            }
            exportedIntPropertyCache.put(exportedProperty, ra);
        }
        return ra;
    }
}
