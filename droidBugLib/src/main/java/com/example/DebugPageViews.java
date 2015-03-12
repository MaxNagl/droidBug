package com.example;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.NanoHTTPD;
import de.siebn.javaBug.XML;

/**
 * Created by Sieben on 04.03.2015.
 */
public class DebugPageViews {
    final Activity activity;

    public DebugPageViews(Activity activity) {
        this.activity = activity;
    }

    private View findView(View view, int hash) {
        if (System.identityHashCode(view) == hash)
            return view;
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = findView(vg.getChildAt(i), hash);
                if (v != null) return v;
            }
        }
        return null;
    }

    @JavaBug.Serve("/viewShot/")
    public NanoHTTPD.Response getViewShot(String uri) {
        int hash = Integer.parseInt(uri.substring(10));
        View view = findView(activity.getWindow().getDecorView(), hash);
        if (view != null) {
            Bitmap bmp = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(bmp));
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 90, bout);
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "image/png", new ByteArrayInputStream(bout.toByteArray()));
        }
        return null;
    }

    @JavaBug.Serve("/viewTree/")
    public String viewTree() {
        XML xhtml = new XML();
        addViewTree(xhtml, activity.getWindow().getDecorView());
        return xhtml.getHtml();
    }

    private void addViewTree(XML xhtml, View view) {
        XML li = xhtml.add("li");
        li.add("a").setHref("/viewShot/" + System.identityHashCode(view)).appendText(view.toString());
        if (view instanceof ViewGroup) {
            XML ul = xhtml.add("ul");
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++)
                addViewTree(ul, vg.getChildAt(i));
        }
    }
}
