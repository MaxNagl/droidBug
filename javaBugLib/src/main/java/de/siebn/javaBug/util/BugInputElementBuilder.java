package de.siebn.javaBug.util;

import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeSelectionAdapter;

public class BugInputElementBuilder {
    @SuppressWarnings("unchecked")
    public static BugInputElement build(Object value, Class clazz, int paramIndex, TypeAdapter typeAdapter) {
        BugInputElement input;
        String text = value == null ? null : typeAdapter.toString(value);
        if (typeAdapter instanceof TypeSelectionAdapter) {
            BugInputList inputList = new BugInputList("p" + paramIndex, null);
            inputList.addList(((TypeSelectionAdapter) typeAdapter).getValues(clazz));
            inputList.text = text;
            input = inputList;
        } else {
            BugInputText bugInput = new BugInputText("p" + paramIndex, null);
            bugInput.text = text;
            if (value == null) bugInput.mode = "null";
            input = bugInput;
        }
        return input;
    }
}
