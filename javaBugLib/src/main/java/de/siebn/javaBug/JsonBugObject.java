package de.siebn.javaBug;

import java.util.ArrayList;
import java.util.List;

public class JsonBugObject extends JsonBugBase {
    public String name;
    public String expand;
    public List<Property> properties;
    public List<Action> actions;

    public JsonBugObject() {
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
}

