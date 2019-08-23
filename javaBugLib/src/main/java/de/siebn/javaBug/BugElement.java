package de.siebn.javaBug;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.*;

public abstract class BugElement {
    public static String ON_CLICK_INVOKE = "invoke";
    public static String ON_CLICK_EXPAND = "expand";

    public final String type;
    public String id;
    public String clazz;
    public String onClick;
    public String hoverGroup;
    public Map<String, String> styles;

    public BugElement() {
        this.type = getClass().getSimpleName();
    }

    public BugElement setId(String id) {
        this.id = id;
        return this;
    }

    public BugElement setClazz(String clazz) {
        this.clazz = clazz;
        return this;
    }

    public BugElement addClazz(String clazz) {
        if (this.clazz == null) {
            this.clazz = clazz;
        } else {
            this.clazz += " " + clazz;
        }
        return this;
    }

    public BugElement format(BugFormat... formats) {
        for (BugFormat format : formats) addClazz(format.clazzes);
        return this;
    }

    public BugElement setOnClick(String onClick) {
        this.onClick = onClick;
        return this;
    }

    public BugElement setStyle(String name, String value) {
        if (styles == null) styles = new LinkedHashMap<>();
        styles.put(name, value);
        return this;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static class BugInclude extends BugElement {
        public String url;

        public BugInclude(String url) {
            this.url = url;
        }

        public BugInclude setUrl(String url) {
            this.url = url;
            return this;
        }
    }

    public static abstract class BugGroup extends BugElement {
        public final List<BugElement> elements = new ArrayList<>();

        public BugGroup add(BugElement element) {
            if (element != null) elements.add(element);
            return this;
        }

        public BugGroup addText(String text) {
            return add(new BugText(text));
        }

        public BugGroup addSpace() {
            return add(BugText.SPACE);
        }
    }

    public static class BugList extends BugGroup {
    }

    public static class BugInlineList extends BugGroup {
    }

    public static class BugDiv extends BugGroup {
    }

    public static class BugEntry extends BugGroup {
        public Object expand;
        public Boolean autoExpand;

        public BugEntry setExpand(Object expand) {
            this.expand = expand;
            return this;
        }
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
        public static BugText VALUE_SEPARATOR = new BugText(": ");
        public static BugText SPACE = new BugText(" ");
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
            text.format(BugFormat.clazz);
            return text;
        }

        public static BugText getForModifier(int modifier) {
            BugText text = new BugText(StringifierUtil.modifiersToString(modifier, null, false));
            text.format(BugFormat.modifier);
            return text;
        }

        public static BugText getForValue(Object val) {
            BugText text = new BugText(TypeAdapters.toString(val));
            text.format(val == null ? BugFormat.nul : BugFormat.value);
            return text;
        }

        public static BugText getForByteSize(long size) {
            BugText text = new BugText(HumanReadable.formatByteSizeBinary(size));
            text.format(BugFormat.value);
            text.setTooltip(String.format(Locale.getDefault(), "%,d B", size));
            return text;
        }
    }

    public static class BugPre extends BugText {
        public BugPre(String text) {
            super(text);
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

    public static class BugImg extends BugGroup {
        public String src;

        public BugImg setSrc(String src) {
            this.src = src;
            return this;
        }
    }

    public static class BugInputList extends BugInputElement {
        public String text;
        public List<Option> options = new ArrayList<>();

        public BugInputList(String callId, String text) {
            this.callId = callId;
            this.text = text;
        }

        public void addMap(Map<String, String> map) {
            for (Entry<String, String> entry : map.entrySet()) {
                options.add(new Option(entry.getKey(), entry.getValue()));
            }
        }

        public static class Option {
            public String id;
            public String text;

            public Option(String id, String text) {
                this.id = id;
                this.text = text;
            }
        }
    }

    public static class BugInputCheckbox extends BugInputElement {
        public String text;
        public String onChange;
        public boolean checked;

        public BugInputCheckbox(String callId, String text) {
            this.callId = callId;
            this.text = text;
        }

        public BugInputCheckbox setOnChange(String onChange) {
            this.onChange = onChange;
            return this;
        }

        public BugInputCheckbox setChecked(boolean checked) {
            this.checked = checked;
            return this;
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

    public static class BugTabs extends BugElement {
        public List<BugTab> tabs = new ArrayList<>();

        public static class BugTab {
            public String title;
            public Object content;
        }
    }

    public static class BugSplit extends BugGroup {
        public static final String ORIENTATION_VERTICAL = "vertical";
        public static final String ORIENTATION_HORIZONTAL = "horizontal";
        public String orientation;

        public BugSplit(String orientation) {
            this.orientation = orientation;
        }

    }

    public static class BugSplitElement extends BugElement {
        public String weight = "1";
        public String fixed = "0";
        public Object content;

        public BugSplitElement(Object content) {
            this.content = content;
        }

        public BugSplitElement setWeight(String weight) {
            this.weight = weight;
            return this;
        }

        public BugSplitElement setFixed(String fixed) {
            this.fixed = fixed;
            return this;
        }

        public BugSplitElement setContent(Object content) {
            this.content = content;
            return this;
        }
    }
}
