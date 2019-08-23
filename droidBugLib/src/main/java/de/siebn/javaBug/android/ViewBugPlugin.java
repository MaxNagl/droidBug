package de.siebn.javaBug.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
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

    @JavaBug.Serve("^/viewsJson")
    public BugElement serveViewsJson() {
        BugSplit horizontal = new BugSplit(BugSplit.ORIENTATION_HORIZONTAL);
        BugSplit vertical = new BugSplit(BugSplit.ORIENTATION_VERTICAL);
        horizontal.add(new BugSplitElement(vertical));
        horizontal.add(new BugSplitElement(new BugDiv().setId("VuewBugDetails").format(BugFormat.paddingNormal)));
        vertical.add(new BugSplitElement("/viewTreeDivsJson").format(BugFormat.paddingNormal));
        vertical.add(new BugSplitElement("/viewTreeJson").format(BugFormat.paddingNormal));
        return horizontal;
    }

    @JavaBug.Serve("^/views")
    public String serveViews() {
        XML div = new XML("div").setAttr("split", "horizontal");
        div.add("div").setAttr("split", "vertical").setAttr("autoload", "/viewcol").appendText("LOADING...");
        div.add("div").setId("viewDetails").setAttr("style", "overflow:auto");
        return div.getXml();
    }

    @JavaBug.Serve("^/viewTreeJson")
    public BugElement serveViewsTree() {
        BugList list = new BugList();
        addViewTree(list, activity.getWindow().getDecorView());
        return list;
    }

    private void addViewTree(BugGroup parent, View view) {
        BugEntry entry = new BugEntry();
        entry.hoverGroup = javaBug.getObjectBug().getObjectReference(view);
        entry.autoExpand = true;
        BugElement title = new BugText(view.toString()).setClazz("title");
        setLoadDetailsOnClick(title, view);
        entry.add(title);
        if (view instanceof ViewGroup) {
            BugList list = new BugList();
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++)
                addViewTree(list, vg.getChildAt(i));
            entry.expand = list;
        }
        parent.add(entry);
    }

    @JavaBug.Serve("^/viewTreeDivsJson")
    public BugElement serveViewDivsTree() {
        View decorView = activity.getWindow().getDecorView();
        BugDiv div = new BugDiv();

        BugImg img = new BugImg();
        setPositionStyle(img, decorView);
        img.setSrc(getLinkToViewShot(decorView));
        div.add(img);

        addViewDivTree(div, decorView);
        return div.format(BugFormat.autoScale, BugFormat.autoScaleCenter);
    }

    private void addViewDivTree(BugGroup parent, View view) {
        BugDiv div = new BugDiv();
        div.hoverGroup = javaBug.getObjectBug().getObjectReference(view);
        setLoadDetailsOnClick(div, view);
        setPositionStyle(div, view);
        div.setStyle("position", "absolute");
        parent.add(div);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++)
                addViewDivTree(div, vg.getChildAt(i));
        }
    }

    private void setLoadDetailsOnClick(BugElement element, View view) {
        String details = javaBug.getObjectBug().getObjectDetailsLink(view);
        element.setOnClick("$('#VuewBugDetails').loadBugElement('" + details + "');");
    }

    private void setPositionStyle(BugElement element, View view) {
        element.setStyle("left", view.getLeft() + "px");
        element.setStyle("top", view.getTop() + "px");
        element.setStyle("width", view.getWidth() + "px");
        element.setStyle("height", view.getHeight() + "px");
    }

    @JavaBug.Serve("^/viewcol")
    public String serveViewsCol() {
        View decorView = activity.getWindow().getDecorView();
        XML root = new XML("root");
        XML imgDiv = root.add("div").setAttr("style", "overflow:hidden").add("div").setAttr("style", "transform-origin:0% 0%").setAttr("autoscale", "true");
        XML img = imgDiv.add("img").setAttr("src", getLinkToViewShot(decorView));
        img.setAttr("width", String.valueOf(decorView.getWidth()));
        img.setAttr("height", String.valueOf(decorView.getHeight()));
        img.setAttr("style", "position:relative");
        img.addElement(serveViewTreeDivs());
        root.add("div").setAttr("style", "overflow:auto").addElement(serveViewTree());
        return root.getChildrenXml();
    }

    @JavaBug.Serve("^/viewTreeDivs")
    public XML serveViewTreeDivs() {
        XML ul = new XML("ul").setAttr("style", "position:absolute;left:0px;top:0px");
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
        li.setAttr("onClick", "loadGet('#viewDetails', '" + javaBug.getObjectBug().getObjectDetailsLink(view) + "', true)");
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
        vDiv.setAttr("style", "position:absolute;left:" + view.getLeft() + "px;width:" + view.getWidth() + "px;top:" + view.getTop() + "px;height:" + view.getHeight() + "px");
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
    public Object getContent() {
        return "/viewsJson";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
