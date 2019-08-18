package de.siebn.javaBug;

import java.util.ArrayList;
import java.util.List;

public class JsonBugEntry extends JsonBugBase {
    public String modifiers;
    public String clazz;
    public String name;
    public String value;
    public String expand;
    public List<Property> properties;
    public List<Action> actions;
    public List<JsonBugBase> elements = new ArrayList<>();

    public JsonBugEntry() {
        super("entry");
    }

    public List<Property> getOrCreateProperties() {
        if (properties == null) properties = new ArrayList<>();
        return properties;
    }

    public List<Action> getOrCreateActions() {
        if (actions == null) actions = new ArrayList<>();
        return actions;
    }

    public List<JsonBugBase> getOrCreateCallables() {
        return elements;
    }

    public static class Property {
        public String name;
        public String value;

        public Property() {
        }

        public Property(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class Action extends JsonBugBase {
        public static String ACTION_DOWNLOAD = "linkNewWindow";

        public String name;
        public String action;
        public String value;

        public Action() {
            super("action");
        }

        public Action(String name, String action, String value) {
            this();
            this.name = name;
            this.action = action;
            this.value = value;
        }
    }

    public static class Callable extends JsonBugBase {
        public static String ACTION_REFRESH_ELEMENTS = "refreshElements";
        public static String ACTION_EXPAND_RESULT = "expandResult";

        public String action;
        public List<Parameter> parameters = new ArrayList<>();
        public String url;
        public boolean parentheses;

        public Callable(String action) {
            super("callable");
            this.action = action;
        }
    }

    public static class Parameter extends JsonBugBase {
        public String id;
        public String name;
        public String clazz;
        public String value;
        public String unit;
        public String refresh;

        public Parameter(String id, String name, String clazz, String value) {
            super("parameter");
            this.id = id;
            this.name = name;
            this.clazz = clazz;
            this.value = value;
        }
    }
}

