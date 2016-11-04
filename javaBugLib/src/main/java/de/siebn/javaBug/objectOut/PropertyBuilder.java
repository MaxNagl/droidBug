package de.siebn.javaBug.objectOut;

import java.util.ArrayList;
import java.util.List;

import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 04.11.2016.
 */

public class PropertyBuilder {
    private ObjectBugPlugin objectBug;

    private String name;
    private List<ParameterBuilder> parameters;

    private ParameterBuilder value;
    private String invokationLink;
    private Integer modifiers;
    private Class<?> type;
    private String expandLink;

    public PropertyBuilder(ObjectBugPlugin objectBug) {
        this.objectBug = objectBug;
    }

    public PropertyBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public ParameterBuilder addParameter() {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }
        ParameterBuilder parameter = new ParameterBuilder();
        parameters.add(parameter);
        return parameter;
    }

    public void build(XML xml) {
        XML li = xml.add("li").setClass("object");
        boolean addedText = false;
        if (modifiers != null) {
            li.addClass(StringifierUtil.modifiersToString(modifiers, "mod", true));
            li.add("span").setClass("modifier").appendText(StringifierUtil.modifiersToString(modifiers, null, false));
            addedText = true;
        }
        if (type != null) {
            if (addedText) li.appendText(" ");
            li.add("span").setClass("type").appendText(type.getSimpleName());
            addedText = true;
        }
        if (name != null) {
            if (addedText) li.appendText(" ");
            li.add("span").setClass("fieldName").appendText(name);
            addedText = true;
        }
        if (parameters != null) {
            li.appendText("(");
            boolean first = true;
            for (ParameterBuilder param : parameters) {
                if (!first) li.appendText(", ");
                param.build(li);
                first = false;
            }
            li.appendText(")");
            addedText = true;
        }
        if (value != null) {
            if (addedText) li.appendText(": ");
            if (value != null) value.build(li);
        }
        if (invokationLink != null) {
            li.setAttr("invoke", invokationLink);
        }
        if (expandLink != null) {
            li.setAttr("expand", expandLink);
        } else {
            li.addClass("notOpenable");
        }
    }

    public ParameterBuilder createValue() {
        value = new ParameterBuilder();
        return value;
    }

    public PropertyBuilder setInvokeLink(String invokationLink) {
        this.invokationLink = invokationLink;
        return this;
    }

    public PropertyBuilder setModifiers(int modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    public PropertyBuilder setType(Class<?> type) {
        this.type = type;
        return this;
    }

    public PropertyBuilder setExpandLink(String expandLink) {
        this.expandLink = expandLink;
        return this;
    }

    public void setExpandObject(Object val, Class<?> type) {
        if (val != null && !type.isPrimitive()) {
            setExpandLink(objectBug.getObjectDetailsLink(val));
        }
    }

    public class ParameterBuilder {
        private String editLink;
        private Object value;
        private boolean nullable;
        private Class<?> type;
        private Integer parameterNum;

        public ParameterBuilder setEditLink(String editLink) {
            this.editLink = editLink;
            return this;
        }

        public void build(XML xml) {
            boolean valueField = value != null || parameterNum != null || editLink != null;
            if (type != null) {
                xml.add("span").setClass("type").appendText(type.getSimpleName());
                if (valueField) xml.appendText(" ");
            }
            if (valueField) {
                XML p = xml.add("span").setClass("parameter");
                if (value != null) {
                    p.appendText(TypeAdapters.getTypeAdapter(value.getClass()).toString(value));
                }
                if (parameterNum != null) {
                    p.setAttr("parameter", "p" + parameterNum);
                }
                if (editLink != null) {
                    p.setAttr("editurl", editLink);
                    if (nullable) p.setAttr("editNullify", "true");
                }
            }
        }

        public ParameterBuilder setValue(Object value) {
            this.value = value;
            return this;
        }

        public ParameterBuilder setNullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public ParameterBuilder setType(Class<?> type) {
            this.type = type;
            return this;
        }

        public ParameterBuilder setParameterNum(int num) {
            this.parameterNum = num;
            return this;
        }
    }
}
