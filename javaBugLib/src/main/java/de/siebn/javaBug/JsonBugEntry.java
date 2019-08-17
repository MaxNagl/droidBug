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
    public Callable callable;

    public JsonBugEntry() {
        super("object");
    }

    public List<Property> getOrCreateProperties() {
        if (properties == null) properties = new ArrayList<>();
        return properties;
    }

    public List<Action> getOrCreateActions() {
        if (actions == null) actions = new ArrayList<>();
        return actions;
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
        public List<Parameter> parameters = new ArrayList<>();
        public String url;
    }

    public static class Parameter {
        public String name;
        public String clazz;
        public String value;

        public Parameter(String name, String clazz, String value) {
            this.name = name;
            this.clazz = clazz;
            this.value = value;
        }
    }
}

