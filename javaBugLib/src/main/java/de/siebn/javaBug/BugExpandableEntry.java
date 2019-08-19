package de.siebn.javaBug;

import java.util.ArrayList;
import java.util.List;

import de.siebn.javaBug.BugElement.BugGroup;

public class BugExpandableEntry extends BugGroup {
    public BugElement title;
    public String expand;

    public static class BugCallable extends BugElement {
        public static String ACTION_REFRESH_ELEMENTS = "refreshElements";
        public static String ACTION_EXPAND_RESULT = "expandResult";

        public String action;
        public List<Parameter> parameters = new ArrayList<>();
        public String url;
        public boolean parentheses;

        public BugCallable(String action) {
            this.action = action;
        }
    }

    public static class Parameter extends BugElement {
        public String id;
        public String name;
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

