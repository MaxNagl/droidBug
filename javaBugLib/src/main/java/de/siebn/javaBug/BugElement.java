package de.siebn.javaBug;

import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.typeAdapter.TypeAdapters.StringPair;
import de.siebn.javaBug.util.*;
import de.siebn.javaBug.util.BugJsonWriter.BugJsonElement;

public abstract class BugElement {
    public static String ON_CLICK_INVOKE = "invoke";
    public static String ON_CLICK_EXPAND = "expand";

    public final String type;
    public String id;
    public String clazz;
    public String onClick;
    public String hoverGroup;
    public String reference;
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

    public BugElement setReference(String reference) {
        this.reference = reference;
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
        return new BugJsonWriter(false).writeObject(this).getString();
    }

    public byte[] toGzipJson() {
        return new BugJsonWriter(true).writeObject(this).getBytes();
    }

    public void writeJsonFields(BugJsonWriter writer) {
        writer.wrtieField("id", id);
        writer.wrtieField("clazz", clazz);
        writer.wrtieField("onClick", onClick);
        writer.wrtieField("hoverGroup", hoverGroup);
        writer.wrtieField("reference", reference);
        writer.wrtieField("styles", styles);
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

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("url", url);
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

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("elements", elements);
        }
    }

    public static class BugList extends BugGroup {
    }

    public static class BugInlineList extends BugGroup {
    }

    public static class BugDiv extends BugGroup {
    }

    public static class BugEntry extends BugGroup {
        public BugElement expand;
        public Boolean autoExpand;

        public BugEntry setExpand(BugElement expand) {
            this.expand = expand;
            return this;
        }

        public BugEntry setExpandInclude(String url) {
            if (url == null) return this;
            return setExpand(new BugInclude(url));
        }

        public BugEntry setAutoExpand(boolean autoExpand) {
            this.autoExpand = autoExpand;
            return this;
        }

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("expand", expand);
            writer.wrtieField("autoExpand", autoExpand);
        }
    }

    public static class BugInputElement extends BugElement {
        public String refreshUrl;
        public String callId;
        public Boolean enabled;

        public BugInputElement setRefreshUrl(String refreshUrl) {
            this.refreshUrl = refreshUrl;
            return this;
        }

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("refreshUrl", refreshUrl);
            writer.wrtieField("callId", callId);
            writer.wrtieField("enabled", enabled);
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

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("text", text);
            writer.wrtieField("tooltip", tooltip);
        }

        public static BugText getForClass(Class<?> clazz) {
            BugText text = new BugText(clazz.getSimpleName());
            text.setTooltip(clazz.getName());
            text.format(BugFormat.clazz);
            return text;
        }

        private final static HashMap<Integer, BugText> modifierCache = new HashMap<>();
        public static BugText getForModifier(int modifier) {
            BugText text = modifierCache.get(modifier);
            if (text == null) {
                text = new BugText(StringifierUtil.modifiersToString(modifier, null, false));
                text.format(BugFormat.modifier);
                modifierCache.put(modifier, text);
            }
            return text;
        }

        public static BugText getForMethod(Method method) {
            BugText text = new BugText(method.getName());
            text.setTooltip(method.toString());
            text.format(BugFormat.method);
            return text;
        }

        public static BugText getForValue(Object val) {
            BugText text = new BugText(val == null ? "null" : TypeAdapters.toString(val));
            text.setReference(BugObjectCache.getReference(val));
            text.hoverGroup = BugObjectCache.getReference(val);
            if (val != null) text.setTooltip(val.getClass().getName());
            return text;
        }

        public static BugText getForValueFormated(Object val) {
            return getForValueFormated(val, BugFormat.value, BugFormat.nul);
        }

        public static BugText getForValueFormated(Object val, BugFormat format) {
            return (BugText) getForValue(val).format(format);
        }

        public static BugText getForValueFormated(Object val, BugFormat format, BugFormat nullFormat) {
            return (BugText) getForValue(val).format(val == null ? nullFormat : format);
        }

        public static BugText getForByteSize(long size) {
            BugText text = new BugText(HumanReadable.formatByteSizeBinary(size));
            text.format(BugFormat.value);
            text.setTooltip(String.format(Locale.getDefault(), "%,d B", size));
            return text;
        }
    }

    public static class BugPre extends BugText {
        public String stream;

        public BugPre(String text) {
            super(text);
        }

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("stream", stream);
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

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("url", url);
        }
    }

    public static class BugInputText extends BugText {
        public boolean nullable = true;
        public boolean scriptable = true;
        public boolean referenceable = true;
        public boolean textable = true;
        public String mode;

        public BugInputText(String callId, String text) {
            super(text);
            this.callId = callId;
            enabled = true;
        }

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("nullable", nullable);
            writer.wrtieField("scriptable", scriptable);
            writer.wrtieField("referenceable", referenceable);
            writer.wrtieField("textable", textable);
            writer.wrtieField("mode", mode);
        }
    }

    public static class BugImg extends BugGroup {
        public String src;

        public BugImg setSrc(String src) {
            this.src = src;
            return this;
        }

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("src", src);
        }
    }

    public static class BugInputList extends BugInputElement {
        public String text;
        public List<Option> options = new ArrayList<>();

        public BugInputList(String callId, String text) {
            this.callId = callId;
            this.text = text;
        }

        public void addList(List<StringPair> list) {
            for (StringPair pair : list) {
                options.add(new Option(pair.key, pair.value));
            }
        }

        public static class Option implements BugJsonElement {
            public String id;
            public String text;

            public Option(String id, String text) {
                this.id = id;
                this.text = text;
            }

            @Override
            public void writeJsonFields(BugJsonWriter writer) {
                writer.wrtieField("id", id);
                writer.wrtieField("text", text);
            }
        }

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("text", text);
            writer.wrtieField("options", options);
        }
    }

    public static class BugInputCheckbox extends BugInputElement {
        public String text;
        public String onChange;
        public boolean checked;

        public BugInputCheckbox(String callId, String text) {
            enabled = true;
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

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("text", text);
            writer.wrtieField("options", onChange);
            writer.wrtieField("options", checked);
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

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("action", action);
            writer.wrtieField("url", url);
        }
    }

    public static class BugTabs extends BugElement {
        public List<BugTab> tabs = new ArrayList<>();

        public static class BugTab implements BugJsonElement {
            public String title;
            public BugElement content;

            @Override
            public void writeJsonFields(BugJsonWriter writer) {
                writer.wrtieField("title", title);
                writer.wrtieField("content", content);
            }
        }

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("tabs", tabs);
        }
    }

    public static class BugSplit extends BugGroup {
        public static final String ORIENTATION_VERTICAL = "vertical";
        public static final String ORIENTATION_HORIZONTAL = "horizontal";
        public String orientation;

        public BugSplit(String orientation) {
            this.orientation = orientation;
        }

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("orientation", orientation);
        }
    }

    public static class BugSplitElement extends BugElement {
        public static final BugSplitElement RESIZE_HANDLE = (BugSplitElement) new BugSplitElement(null).setSplitType(BugSplitElement.TYPE_RESIZE_HANDLE).setFixed("auto").setWeight("0").format(BugFormat.resizeHandle);
        public static final String TYPE_RESIZE_HANDLE = "resizeHandle";

        public String weight = "1";
        public String fixed = "0";
        public String splitType;
        public BugElement content;

        public BugSplitElement(BugElement content) {
            this.content = content;
        }

        public BugSplitElement setSplitType(String splitType) {
            this.splitType = splitType;
            return this;
        }

        public BugSplitElement setWeight(String weight) {
            this.weight = weight;
            return this;
        }

        public BugSplitElement setFixed(String fixed) {
            this.fixed = fixed;
            return this;
        }

        public BugSplitElement setContent(BugElement content) {
            this.content = content;
            return this;
        }

        @Override
        public void writeJsonFields(BugJsonWriter writer) {
            super.writeJsonFields(writer);
            writer.wrtieField("weight", weight);
            writer.wrtieField("fixed", fixed);
            writer.wrtieField("content", content);
            writer.wrtieField("splitType", splitType);
        }
    }
}
