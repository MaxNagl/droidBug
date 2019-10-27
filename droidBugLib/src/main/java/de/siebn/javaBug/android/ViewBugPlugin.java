package de.siebn.javaBug.android;

import android.app.Activity;
import android.graphics.*;
import android.view.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Map;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.plugins.RootBugPlugin;
import de.siebn.javaBug.util.BugObjectCache;

/**
 * Created by Sieben on 04.03.2015.
 */
public class ViewBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBugCore javaBug;
    private Activity activity;

    public ViewBugPlugin(JavaBugCore javaBug, Activity activity) {
        this.javaBug = javaBug;
        this.activity = activity;
    }

    public void setActivity(Activity activity) {
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

    @JavaBugCore.Serve("^/viewShot/([^/]*)")
    public NanoHTTPD.Response serveViewShot(String[] params, NanoHTTPD.IHTTPSession session) {
        Map<String, String> parms = session.getParms();
        boolean noChildren = parms.get("noChildren") != null;
        boolean cropVisible = parms.get("cropVisible") != null;
        int hash = Integer.parseInt(params[1], 16);
        View view = findView(activity.getWindow().getDecorView(), hash);
        if (view != null) {
            int width = view.getWidth(), height = view.getHeight();
            int left = 0, top = 0;
            if (cropVisible) {
                Rect visible = new Rect();
                Point offset = new Point();
                if (view.getGlobalVisibleRect(visible, offset)) {
                    width = visible.width();
                    height = visible.height();
                    left = visible.left - offset.x;
                    top = visible.top - offset.y;
                }
            }
            Bitmap bmp = Bitmap.createBitmap(Math.max(width, 1), Math.max(height, 1), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.translate(-left, -top);
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
                view.draw(canvas);
                for (View child : visibles) child.setVisibility(View.VISIBLE);
            } else {
                view.draw(canvas);
            }
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 90, bout);
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "image/png", new ByteArrayInputStream(bout.toByteArray()));
        }
        return null;
    }

    public String getLinkToViewShot(View view, boolean noChildren, boolean cropVisible) {
        String link = "/viewShot/" + Integer.toHexString(System.identityHashCode(view)) + "?" + System.currentTimeMillis();
        if (noChildren) link += "&noChildren";
        if (cropVisible) link += "&cropVisible";
        return link;
    }

    @JavaBugCore.Serve("^/views")
    public BugElement serveViews() {
        BugSplit horizontal = new BugSplit(BugSplit.ORIENTATION_HORIZONTAL);
        BugSplit left = new BugSplit(BugSplit.ORIENTATION_VERTICAL);
        BugSplit right = new BugSplit(BugSplit.ORIENTATION_VERTICAL);
        horizontal.add(new BugSplitElement(left));
        horizontal.add(BugSplitElement.RESIZE_HANDLE);
        horizontal.add(new BugSplitElement(right));
        left.add(new BugSplitElement(getControlElements()).setWeight("0").setFixed("auto").format(BugFormat.paddingNormal));
        left.add(new BugSplitElement(new BugInclude("/viewTreeLayers")).setId("ViewBugViewTreeLayers").format(BugFormat.paddingNormal));
        right.add(new BugSplitElement(new BugInclude("/viewTree")).setId("ViewBugViewTree").format(BugFormat.paddingNormal));
        right.add(BugSplitElement.RESIZE_HANDLE);
        right.add(new BugSplitElement(new BugDiv().setId("ViewBugDetails").format(BugFormat.paddingNormal)));
        return horizontal;
    }

    public BugElement getControlElements() {
        BugList list = new BugList();
        String reloadViewTree = "$('#ViewBugViewTree').loadContent(" + new BugInclude("/viewTree").toJson() + ", 'application/json');";
        String reloadViewTreeLayersInclude = "$('#ViewBugViewTreeLayers').loadContent(" + new BugInclude("/viewTreeLayers").toJson() + ", 'application/json');";
        String reloadViewTree3dLayersInclude = "$('#ViewBugViewTreeLayers').loadContent(" + new BugInclude("/viewTreeLayers?layers3d").toJson() + ", 'application/json');";
        String reloadViewTree3dCroppedLayersInclude = "$('#ViewBugViewTreeLayers').loadContent(" + new BugInclude("/viewTreeLayers?layers3d&cropVisible").toJson() + ", 'application/json');";
        list.add(new BugText("Refresh 2D").format(BugFormat.button).setOnClick(reloadViewTree + reloadViewTreeLayersInclude));
        list.add(new BugText("Refresh 3D").format(BugFormat.button).setOnClick(reloadViewTree + reloadViewTree3dCroppedLayersInclude));
        list.add(new BugText("Refresh 3D full views").format(BugFormat.button).setOnClick(reloadViewTree + reloadViewTree3dLayersInclude));
        list.setStyle("text-align", "center");
        return list;
    }

    @JavaBugCore.Serve("^/viewTree")
    public BugElement serveViewsTree() {
        BugList list = new BugList();
        addViewTree(list, activity.getWindow().getDecorView());
        return list;
    }

    private void addViewTree(BugGroup parent, View view) {
        BugEntry entry = new BugEntry();
        entry.hoverGroup = BugObjectCache.getReference(view);
        entry.autoExpand = true;
        BugElement title = BugText.getForValueFormated(view);
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

    @JavaBugCore.Serve("^/viewTreeLayers")
    public BugElement serveViewTreeLayers(NanoHTTPD.IHTTPSession session) {
        Map<String, String> parms = session.getParms();
        boolean layers3d = parms.get("layers3d") != null;
        boolean cropVisible = parms.get("cropVisible") != null;

        View decorView = activity.getWindow().getDecorView();
        BugDiv div = new BugDiv();

        if (layers3d) {
            BugDiv treeHolder = new BugDiv();
            setPositionStyle(treeHolder, decorView, false);
            treeHolder.addClazz("root3d");
            treeHolder.setStyle("transform", "rotateY(45deg)");
            treeHolder.setStyle("transform-style", "preserve-3d");
            addViewDivTree(treeHolder, decorView, true, cropVisible, 0);
            div.add(treeHolder);
            div.setStyle("perspective", "10000px");
        } else {
            BugImg img = new BugImg();
            setPositionStyle(img, decorView, false);
            img.setSrc(getLinkToViewShot(decorView, false, false));
            div.add(img);
            addViewDivTree(div, decorView, false, false, 0);
        }

        return div.format(BugFormat.autoScale, BugFormat.autoScaleCenter);
    }

    private BugDiv addViewDivTree(BugGroup bugGroup, View view, boolean images, boolean cropVisible, int depth) {
        BugDiv div = new BugDiv();
        div.hoverGroup = BugObjectCache.getReference(view);
        setLoadDetailsOnClick(div, view);
        setPositionStyle(div, view, cropVisible);
        if (images) {
            div.addClazz("layer3d");
            int color = Color.HSVToColor(new float[]{(depth * 77) % 360, 1, 1});
            div.setStyle("background", "rgba(" + Color.red(color) + ", " + Color.green(color) + ", " + Color.blue(color) + ", 0.25) url(\"" + getLinkToViewShot(view, true, cropVisible) + "\")");
            div.setStyle("border", "1px solid rgba(" + Color.red(color) + ", " + Color.green(color) + ", " + Color.blue(color) + ", 0.5)");
        } else {
            div.setStyle("position", "absolute");
        }
        bugGroup.add(div);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (View v : BugViewHelper.getSortedChildren(vg))
                addViewDivTree(div, v, images, cropVisible, depth + 1);
        }
        return div;
    }

    private void setLoadDetailsOnClick(BugElement element, View view) {
        BugInclude include = new BugInclude(javaBug.getObjectBug().getObjectDetailsLink(view));
        element.setOnClick("$('#ViewBugDetails').loadContent('" + include.toJson() + "', 'application/json');");
    }

    private void setPositionStyle(BugElement element, View view, boolean cropped) {
        int left = view.getLeft();
        int top = view.getTop();
        int width = view.getWidth();
        int height = view.getHeight();
        Rect visible = new Rect();
        Rect parentVisible = new Rect();
        ViewParent parent = view.getParent();
        boolean isVisible = view.getGlobalVisibleRect(visible);
        if (parent instanceof View) {
            if (cropped && isVisible && ((View) parent).getGlobalVisibleRect(parentVisible)) {
                left = visible.left - parentVisible.left;
                top = visible.top - parentVisible.top;
            } else {
                left -= ((View) parent).getScrollX();
                top -= ((View) parent).getScrollY();
            }
        }
        if (cropped) {
            if (isVisible) {
                width = visible.width();
                height = visible.height();
            } else {
                width = 0;
                height = 0;
            }
        }
        element.setStyle("left", left + "px");
        element.setStyle("top", top + "px");
        element.setStyle("width", width + "px");
        element.setStyle("height", height + "px");
    }

    @Override
    public String getTabName() {
        return "Views";
    }

    @Override
    public String getTabId() {
        return "views";
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
