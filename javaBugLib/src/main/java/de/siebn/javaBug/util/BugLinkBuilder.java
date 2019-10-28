package de.siebn.javaBug.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
            sb.append(sb.length() == 0 ? '?' : "&").append(encode(e.getKey()));
            if (e.getValue() != null) sb.append("=").append(encode(e.getValue()));
        }
        sb.insert(0, path);
        return sb.toString();
    }

    private String encode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
