package de.siebn.javaBug;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.StringifierUtil;

public abstract class BugElement {
    public final String type;
    public String clazz;

    public BugElement() {
        this.type = getClass().getSimpleName();
    }

    public BugElement setClazz(String clazz) {
        this.clazz = clazz;
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

    public static class BugText extends BugElement {
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

        public static BugText getValueSeparator() {
            BugText text = new BugText(":");
            text.setClazz("separator");
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
}
