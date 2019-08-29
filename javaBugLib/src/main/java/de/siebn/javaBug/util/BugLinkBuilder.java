package de.siebn.javaBug.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class BugLinkBuilder {
    private final String path;
    private Map<String, String> parameters;

    public BugLinkBuilder(String path) {
        this.path = path;
    }

    @SuppressWarnings("unchecked")
    protected <T extends BugLinkBuilder> T setParameter(String key, String value) {
        if (parameters == null) parameters = new LinkedHashMap<>();
        parameters.put(key, value);
        return (T) this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> e : parameters.entrySet()) {
            sb.append(sb.length() == 0 ? '?' : "&").append(e.getKey());
            if (e.getValue() != null) sb.append("=").append(e.getValue());
        }
        sb.insert(0, path);
        return sb.toString();
    }
}
