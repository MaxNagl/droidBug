package de.siebn.javaBug;

import com.google.gson.Gson;

public class JsonBugBase {
    public final String type;

    public JsonBugBase(String type) {
        this.type = type;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
