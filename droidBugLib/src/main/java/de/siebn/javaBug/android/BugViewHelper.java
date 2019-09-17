package de.siebn.javaBug.android;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;

import java.util.*;

public class BugViewHelper {
    public static ArrayList<View> getSortedChildren(ViewGroup viewGroup) {
        final int childrenCount = viewGroup.getChildCount();
        ArrayList<View> list = new ArrayList<>(childrenCount);
        for (int i = 0; i < childrenCount; i++) list.add(viewGroup.getChildAt(i));
        Collections.sort(list, new Comparator<View>(){
            @Override
            public int compare(View o1, View o2) {
                return (int) Math.signum(getZ(o1) - getZ(o2));
            }
        });
        return list;
    }

    @SuppressLint("NewApi")
    public static float getZ(View view) {
        try {
            return view.getZ();
        } catch (Exception e) {
            return 0;
        }

    }
}
