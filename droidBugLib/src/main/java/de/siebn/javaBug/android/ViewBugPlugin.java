package de.siebn.javaBug.android;

import android.app.Activity;
import android.graphics.*;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.plugins.RootBugPlugin;
import de.siebn.javaBug.util.BugObjectCache;

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
    public NanoHTTPD.Response serveViewShot(String[] params, NanoHTTPD.IHTTPSession session) {
        boolean noChildren = session.getParms().get("noChildren") != null;
        int hash = Integer.parseInt(params[1], 16);
        View view = findView(activity.getWindow().getDecorView(), hash);
        if (view != null) {
            Bitmap bmp = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            if (noChildren && view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                HashSet<View> visibles = new HashSet<>();
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    if (child.getVisibility() == View.VISIBLE) {
                        child.setVisibility(View.INVISIBLE);
                        visibles.add(child);
                    }
                }
                view.draw(new Canvas(bmp));
                for (View child : visibles) child.setVisibility(View.VISIBLE);
            } else {
                view.draw(new Canvas(bmp));
            }
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
    public BugElement serveViews() {
        BugSplit horizontal = new BugSplit(BugSplit.ORIENTATION_HORIZONTAL);
        BugSplit vertical = new BugSplit(BugSplit.ORIENTATION_VERTICAL);
        horizontal.add(new BugSplitElement(vertical));
        horizontal.add(new BugSplitElement(new BugDiv().setId("ViewBugDetails").format(BugFormat.paddingNormal)));
        vertical.add(new BugSplitElement(new BugInclude("/viewTreeImageLayers")).format(BugFormat.paddingNormal));
        vertical.add(new BugSplitElement(new BugInclude("/viewTree")).format(BugFormat.paddingNormal));
        return horizontal;
    }

    @JavaBug.Serve("^/viewTree")
    public BugElement serveViewsTree() {
        BugList list = new BugList();
        addViewTree(list, activity.getWindow().getDecorView());
        return list;
    }

    private void addViewTree(BugGroup parent, View view) {
        BugEntry entry = new BugEntry();
        entry.hoverGroup = BugObjectCache.getReference(view);
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

    @JavaBug.Serve("^/viewTreeLayers")
    public BugElement serveViewTreeLayers() {
        View decorView = activity.getWindow().getDecorView();
        BugDiv div = new BugDiv();

        BugImg img = new BugImg();
        setPositionStyle(img, decorView, 0, 0);
        img.setSrc(getLinkToViewShot(decorView));
        div.add(img);

        addViewDivTree(div, decorView, false, 0);
        return div.format(BugFormat.autoScale, BugFormat.autoScaleCenter);
    }

    @JavaBug.Serve("^/viewTreeImageLayers")
    public BugElement serveViewTreeImageLayers() {
        View decorView = activity.getWindow().getDecorView();
        BugDiv div = new BugDiv();
        div.setStyle("perspective", "10000px");

        BugDiv treeHolder = new BugDiv();
        setPositionStyle(treeHolder, decorView, 0, 0);
        treeHolder.addClazz("root3d");
        treeHolder.setStyle("transform", "rotateY(45deg)");
        treeHolder.setStyle("transform-style", "preserve-3d");
        addViewDivTree(treeHolder, decorView, true, 0);
        div.add(treeHolder);
        return div.format(BugFormat.autoScale, BugFormat.autoScaleCenter);
    }

    private BugDiv addViewDivTree(BugGroup parent, View view, boolean images, int depth) {
        BugDiv div = new BugDiv();
        div.hoverGroup = BugObjectCache.getReference(view);
        setLoadDetailsOnClick(div, view);
        setPositionStyle(div, view, 0, 0);
        if (images) {
            div.addClazz("layer3d");
            int color = Color.HSVToColor(new float[]{(depth * 77) % 360, 1, 1});
            div.setStyle("background", "rgba(" + Color.red(color) + ", " + Color.green(color) + ", " + Color.blue(color) + ", 0.25) url(\"" + getLinkToViewShot(view) + "?noChildren\")");
            div.setStyle("border", "1px solid rgba(" + Color.red(color) + ", " + Color.green(color) + ", " + Color.blue(color) + ", 0.5)");
        } else {
            div.setStyle("position", "absolute");
        }
        parent.add(div);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++)
                addViewDivTree(div, vg.getChildAt(i), images, depth + 1);
        }
        return div;
    }

    private void setLoadDetailsOnClick(BugElement element, View view) {
        BugInclude include = new BugInclude(javaBug.getObjectBug().getObjectDetailsLink(view));
        element.setOnClick("$('#ViewBugDetails').loadContent('" + include.toJson() + "', 'application/json');");
    }

    private void setPositionStyle(BugElement element, View view, int offsetLeft, int offsetTop) {
        element.setStyle("left", (offsetLeft + view.getLeft()) + "px");
        element.setStyle("top", (offsetTop + view.getTop()) + "px");
        element.setStyle("width", view.getWidth() + "px");
        element.setStyle("height", view.getHeight() + "px");
    }

    @Override
    public String getTabName() {
        return "Views";
    }

    @Override
    public BugElement getContent() {
        return new BugInclude("/views");
    }

    @Override
    public int getOrder() {
        return -2000;
    }
}
