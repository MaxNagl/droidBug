package de.siebn.javaBug;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.StringifierUtil;

public abstract class BugElement {
    public static String ON_CLICK_INVOKE = "invoke";

    public final String type;
    public String clazz;
    public String onClick;

    public BugElement() {
        this.type = getClass().getSimpleName();
    }

    public BugElement setClazz(String clazz) {
        this.clazz = clazz;
        return this;
    }

    public BugElement setOnClick(String onClick) {
        this.onClick = onClick;
        return this;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static abstract class BugGroup extends BugElement {
        public final List<BugElement> elements = new ArrayList<>();
    }

    public static class BugList extends BugGroup {
    }

    public static class BugInlineList extends BugGroup {
    }

    public static class BugExpandableEntry extends BugGroup {
        public BugElement title;
        public String expand;
    }

    public static class BugText extends BugElement {
        public static BugText VALUE_SEPARATOR = (BugText) new BugText(":").setClazz("separator");
        public static BugText NBSP = new BugText("&nbsp;");

        public String text;
        public String tooltip;
        public String refreshUrl;

        public BugText(String text) {
            this.text = text;
        }

        public BugText setTooltip(String tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public BugText setRefreshUrl(String refreshUrl) {
            this.refreshUrl = refreshUrl;
            return this;
        }

        public static BugText getForClass(Class<?> clazz) {
            BugText text = new BugText(clazz.getSimpleName());
            text.setTooltip(clazz.getName());
            text.setClazz("clazz");
            return text;
        }

        public static BugText getForModifier(int modifier) {
            BugText text = new BugText(StringifierUtil.modifiersToString(modifier, null, false));
            text.setClazz("modifier");
            return text;
        }

        public static BugText getForValue(Object val) {
            BugText text = new BugText(TypeAdapters.toString(val));
            text.setClazz("value");
            return text;
        }
    }

    public static class BugLink extends BugText {
        public String url;

        public BugLink(String text) {
            super(text);
        }

        public BugLink setUrl(String url) {
            this.url = url;
            return this;
        }
    }

    public static class BugInput extends BugText {
        public String callId;

        public BugInput(String callId, String text) {
            super(text);
            this.callId = callId;
        }
    }

    public static class BugInvokable extends BugGroup {
        public static String ACTION_REFRESH_ELEMENTS = "refreshEntry";
        public static String ACTION_EXPAND_RESULT = "expandResult";

        public String action;
        public String url;

        public BugInvokable(String action) {
            this.action = action;
        }

        public BugInvokable addBraces() {
            elements.add(0, new BugText("("));
            elements.add(new BugText(")"));
            return this;
        }

        public static BugInvokable getExpandRefresh(String url) {
            BugInvokable invokable = new BugInvokable(BugInvokable.ACTION_EXPAND_RESULT);
            invokable.url = url;
            invokable.elements.add(new BugText("&#x27F3;").setOnClick(BugElement.ON_CLICK_INVOKE));
            invokable.clazz = "hideCollapsed";
            return invokable;
        }
    }
}
