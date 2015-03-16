package de.siebn.javaBug.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import de.siebn.javaBug.*;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.plugins.RootBugPlugin;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 04.03.2015.
 */
public class ViewBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBug javaBug;
    private final Activity activity;

    public ViewBugPlugin(JavaBug javaBug, Activity activity) {
        this.javaBug = javaBug;
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

    @JavaBug.Serve("^/viewShot/([^/]*)")
    public NanoHTTPD.Response serveViewShot(String[] params) {
        int hash = Integer.parseInt(params[1], 16);
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

    public String getLinkToViewShot(View view) {
        return "/viewShot/" + Integer.toHexString(System.identityHashCode(view));
    }

    @JavaBug.Serve("^/views")
    public String serveViews() {
        XML div = new XML("div").setAttr("split", "horizontal");
        div.add("div").setAttr("split", "vertical").setAttr("autoload", "/viewcol").appendText("LOADING...");
        div.add("div").setId("viewDetails").setAttr("style", "overflow:auto");
        return div.getXml();
    }

    @JavaBug.Serve("^/viewcol")
    public String serveViewsCol() {
        View decorView = activity.getWindow().getDecorView();
        XML root = new XML("root");
        XML imgDiv = root.add("div").setAttr("style", "overflow:hidden").add("div").setAttr("style", "transform-origin:0% 0%").setAttr("autoscale", "true");
        XML img = imgDiv.add("img").setAttr("src", getLinkToViewShot(decorView));
        img.setAttr("width", String.valueOf(decorView.getWidth()));
        img.setAttr("height", String.valueOf(decorView.getHeight()));
        img.setAttr("style","position:relative");
        img.addElement(serveViewTreeDivs());
        root.add("div").setAttr("style", "overflow:auto").addElement(serveViewTree());
        return root.getChildrenXml();
    }

    @JavaBug.Serve("^/viewTreeDivs")
    public XML serveViewTreeDivs() {
        XML ul = new XML("ul").setAttr("style","position:absolute;left:0px;top:0px");
        addViewTreeDiv(ul, activity.getWindow().getDecorView());
        return ul;
    }

    @JavaBug.Serve("^/viewTree")
    public XML serveViewTree() {
        XML ul = new XML("ul");
        addViewTree(ul, activity.getWindow().getDecorView());
        return ul;
    }

    private void addViewTree(XML ul, View view) {
        XML li = ul.add("li").setClass("object");
        //li.add("a").setHref("/viewShot/" + Integer.toHexString(System.identityHashCode(view))).appendText(view.toString());
        li.setAttr("onClick", "load('#viewDetails', '" + javaBug.getObjectBug().getObjectDetailsLink(view) + "', true)");
        li.appendText(view.toString());
        li.setAttr("hoverGroup", Integer.toHexString(System.identityHashCode(view)));
        if (view instanceof ViewGroup) {
            XML cul = li.add("ul").setClass("expand");
            li.setAttr("expand", "true");
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++)
                addViewTree(cul, vg.getChildAt(i));
        } else {
            li.addClass("notOpenable");
        }
    }

    private void addViewTreeDiv(XML div, View view) {
        XML vDiv = div.add("div");
        vDiv.setAttr("style", "position:absolute;left:" + view.getLeft() + "px;width:" + view.getWidth() + "px;top:" + view.getTop() + "px;height:" + view.getHeight() +"px");
        vDiv.setClass("viewRect");
        vDiv.setAttr("hoverGroup", Integer.toHexString(System.identityHashCode(view)));
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++)
                addViewTreeDiv(vDiv, vg.getChildAt(i));
        }
    }

    @Override
    public String getTabName() {
        return "Views";
    }

    @Override
    public String getUrl() {
        return "/views";
    }

    @Override
    public String getTagClass() {
        return "Views";
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
