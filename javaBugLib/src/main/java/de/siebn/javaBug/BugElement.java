package de.siebn.javaBug;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.StringifierUtil;

public abstract class BugElement {
    public static String ON_CLICK_INVOKE = "invoke";
    public static String ON_CLICK_EXPAND = "expand";

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
        public String expand;
        public boolean autoExpand;
    }

    public static class BugInputElement extends BugElement {
        public String refreshUrl;
        public String callId;
        public boolean enabled = true;

        public BugInputElement setRefreshUrl(String refreshUrl) {
            this.refreshUrl = refreshUrl;
            return this;
        }
    }

    public static class BugText extends BugInputElement {
        public static BugText VALUE_SEPARATOR = (BugText) new BugText(":").setClazz("separator");
        public static BugText NBSP = new BugText(UnicodeCharacters.NBSP);
        public static BugText INVOKER = (BugText) new BugText(UnicodeCharacters.INVOKE).setOnClick(BugElement.ON_CLICK_INVOKE);

        public String text;
        public String tooltip;

        public BugText(String text) {
            this.text = text;
        }

        public BugText setTooltip(String tooltip) {
            this.tooltip = tooltip;
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

    public static class BugInputText extends BugText {
        public boolean nullable;

        public BugInputText(String callId, String text) {
            super(text);
            this.callId = callId;
        }
    }

    public static class BugInputList extends BugInputElement {
        public String text;
        public List<BugOption> options = new ArrayList<>();

        public BugInputList(String callId, String text) {
            this.callId = callId;
            this.text = text;
        }

        public void addMap(Map<String, String> map) {
            for (Entry<String, String> entry : map.entrySet()) {
                options.add(new BugOption(entry.getKey(), entry.getValue()));
            }
        }
    }

    public static class BugOption {
        public String id;
        public String text;

        public BugOption(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    public static class BugInvokable extends BugGroup {
        public static String ACTION_SET_VALUE = "setValue";
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
            invokable.elements.add(new BugText(UnicodeCharacters.REFRESH).setOnClick(BugElement.ON_CLICK_INVOKE));
            invokable.clazz = "hideCollapsed";
            return invokable;
        }
    }
}
