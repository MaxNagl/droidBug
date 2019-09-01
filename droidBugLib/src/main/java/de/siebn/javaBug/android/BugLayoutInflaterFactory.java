package de.siebn.javaBug.android;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.LayoutInflater.Factory2;
import android.view.View;

import net.bytebuddy.android.AndroidClassLoadingStrategy;

import java.util.ArrayList;

import de.siebn.javaBug.util.BugByteCodeUtil;

public class BugLayoutInflaterFactory implements Factory2 {
    private final Factory2 parentFactory;

    private final static ArrayList<String> classPrefixes = new ArrayList<>();

    static {
        classPrefixes.add("");
        classPrefixes.add("android.view.");
        classPrefixes.add("android.widget.");
    }

    public BugLayoutInflaterFactory(Factory2 parentFactory) {
        this.parentFactory = parentFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        try {
            Class viewClass = null;
            for (String prefix : classPrefixes) {
                try {
                    viewClass = Class.forName(prefix + name);
                    break;
                } catch (Exception e) {
                }
            }
            if (viewClass != null) {
                BugByteCodeUtil.CLASS_LOADING_STRATEGY = new AndroidClassLoadingStrategy.Wrapping(context.getApplicationContext().getCacheDir());
                return (View) BugByteCodeUtil.getBuggedInstance(viewClass, context, attrs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parentFactory == null ? null : parentFactory.onCreateView(parent, name, context, attrs);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return onCreateView(null, name, context, attrs);
    }

    public static LayoutInflater wrapInflater(final LayoutInflater inflater) {
        LayoutInflater li = inflater.cloneInContext(inflater.getContext());
        li.setFactory2(new BugLayoutInflaterFactory(li.getFactory2()));
        return li;
    }
}
