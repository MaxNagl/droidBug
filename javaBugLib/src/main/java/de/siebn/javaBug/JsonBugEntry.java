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
    public List<Callable> callables;

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

    public List<Callable> getOrCreateCallables() {
        if (callables == null) callables = new ArrayList<>();
        return callables;
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

    public static class Action {
        public static String ACTION_DOWNLOAD = "linkNewWindow";

        public String name;
        public String action;
        public String value;

        public Action() {
        }

        public Action(String name, String action, String value) {
            this.name = name;
            this.action = action;
            this.value = value;
        }
    }

    public static class Callable {
        public static String TYPE_REFRESH_CALLABLES = "refreshCallables";
        public static String TYPE_EXPAND_RESULT = "expandResult";

        public String type;
        public List<Parameter> parameters = new ArrayList<>();
        public String url;
        public boolean parentheses;

        public Callable(String type) {
            this.type = type;
        }
    }

    public static class Parameter {
        public String id;
        public String name;
        public String clazz;
        public String value;
        public String unit;
        public String refresh;

        public Parameter(String id, String name, String clazz, String value) {
            this.id = id;
            this.name = name;
            this.clazz = clazz;
            this.value = value;
        }
    }
}

