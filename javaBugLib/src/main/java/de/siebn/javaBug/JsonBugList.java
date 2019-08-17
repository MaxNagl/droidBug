package de.siebn.javaBug;

import java.util.ArrayList;

public class JsonBugList extends JsonBugBase {
    public final ArrayList<JsonBugBase> elements = new ArrayList<>();

    public JsonBugList() {
        super("list");
    }
}
