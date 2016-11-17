package de.siebn.javaBug.objectOut;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.typeAdapter.TypeAdapters.TypeAdapter;
import de.siebn.javaBug.util.StringifierUtil;
import de.siebn.javaBug.util.XML;

public class ListItemBuilder {
    private String name;
    private List<ParameterBuilder> parameters;
    private List<ParameterBuilder> values;
    private List<ColumnBuilder> columns;

    private String invokationLink;
    private Integer modifiers;
    private Class<?> type;
    private String expandLink;
    private XML expand;

    private String id = UUID.randomUUID().toString();
    private String refreshLink;

    public XML build(XML xml) {
        XML li = xml == null ? new XML("span") : xml.add("li");
        li.setClass("object");
        boolean addedText = false;
        li.setId(id);
        XML input = li.add("input");
        input.setAttr("type", "checkbox");
        input.addClass("opener");
        input.setId(id + "opener");
        input.setAttr("appendTo", "#" + id);
        input.setAttr("appendId", id + "append");
        XML label = li.add("label");
        label.setAttr("for", id + "opener");
        if (expandLink != null) {
            input.setAttr("append", expandLink);
        }
        if (modifiers != null) {
            li.addClass(StringifierUtil.modifiersToString(modifiers, "mod", true));
            label.add("span").setClass("modifier").appendText(StringifierUtil.modifiersToString(modifiers, null, false));
            addedText = true;
        }
        if (type != null) {
            if (addedText) label.appendText(" ");
            label.add("span").setClass("type").appendText(type.getSimpleName());
            addedText = true;
        }
        if (name != null) {
            if (addedText) label.appendText(" ");
            label.add("span").setClass("fieldName").appendText(name);
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
        if (values != null) {
            if (addedText) li.appendText(":");
            for (ParameterBuilder value : values) {
                li.appendText(" ");
                value.build(li);
            }
            addedText = true;
        }
        if (columns != null) {
            for (ColumnBuilder column : columns) {
                if (addedText) li.appendText(" ");
                column.build(li);
                addedText = true;
            }
        }
        if (refreshLink != null) {
            if (addedText) li.appendText(" ");
            XML refresh = li.add("span").setClass("refresh");
            refresh.setAttr("replaceUrl", refreshLink);
            refresh.setAttr("replace", "#" + id);
            addedText = true;
        }
        if (invokationLink != null) {
            XML span = li.add("span");
            span.addClass("invoke");
            span.setAttr("addParams", "#" + id);
            span.setAttr("append", invokationLink);
            span.setAttr("appendTo", "#" + id);
            span.setAttr("appendId", id + "append");
        }
        if (expand != null) {
            li.addElement(expand);
            input.setAttr("checked", "true");
        }
        return li;
    }

    public ListItemBuilder setName(String name) {
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

    public ColumnBuilder addColumn() {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        ColumnBuilder column = new ColumnBuilder();
        columns.add(column);
        return column;
    }

    public ParameterBuilder addValue() {
        if (values == null) {
            values = new ArrayList<>();
        }
        ParameterBuilder value = new ParameterBuilder();
        values.add(value);
        return value;
    }

    public ListItemBuilder setInvokeLink(String invokationLink) {
        this.invokationLink = invokationLink;
        return this;
    }

    public ListItemBuilder setModifiers(int modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    public ListItemBuilder setType(Class<?> type) {
        this.type = type;
        return this;
    }

    public ListItemBuilder setExpandLink(String expandLink) {
        this.expandLink = expandLink;
        return this;
    }

    public ListItemBuilder setRefreshLink(String refreshLink) {
        this.refreshLink = refreshLink;
        return this;
    }

    public XML createExtended(String tag) {
        expand = new XML(tag);
        expand.setId(id + "append");
        return expand;
    }

    public void setExpandObject(ObjectBugPlugin objectBug, Object val, Class<?> type) {
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
        private String updateLink;
        private TypeAdapter<?> typeAdapter;
        private String unit;

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
                    p.setAttr("param", "p" + parameterNum);
                }
                if (editLink != null) {
                    p.setAttr("editUrl", editLink);
                    if (nullable) p.setAttr("editNullify", "true");
                }
                if (updateLink != null) {
                    p.setAttr("updateUrl", updateLink);
                }
                p.setAttr("updateAll", "#" + ListItemBuilder.this.id);
                if (unit != null) {
                    xml.appendText(unit);
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

        public ParameterBuilder setEditLink(String editLink) {
            this.editLink = editLink;
            return this;
        }

        public ParameterBuilder setUpdateLink(String udateLink) {
            this.updateLink = udateLink;
            return this;
        }

        public ParameterBuilder setTypeAdapter(TypeAdapter<?> typeAdapter) {
            this.typeAdapter = typeAdapter;
            return this;
        }

        public ParameterBuilder setUnit(String unit) {
            this.unit = unit;
            return this;
        }
    }

    public class ColumnBuilder {
        private String text;
        private String clazz;
        private String link;
        private String appendLink;

        public ColumnBuilder setText(String text) {
            this.text = text;
            return this;
        }

        public ColumnBuilder setClass(String clazz) {
            this.clazz = clazz;
            return this;
        }

        public ColumnBuilder setLink(String link) {
            this.link = link;
            return this;
        }

        public ColumnBuilder setAppendLink(String appendLink) {
            this.appendLink = appendLink;
            return this;
        }

        public void build(XML xml) {
            XML span = xml.add("span").setClass("column");
            if (clazz != null) span.addClass(clazz);
            if (appendLink != null) {
                String uniqueID = UUID.randomUUID().toString();
                XML input = span.add("input");
                input.setId(uniqueID);
                input.setAttr("append", appendLink);
                input.setAttr("appendTo", "#" + ListItemBuilder.this.id);
                input.setAttr("type", "checkbox");
                XML label = span.add("label");
                label.setAttr("for", uniqueID);
                label.appendText(text);
            } else {
                if (text != null) span.appendText(text);
            }
            if (link != null) {
                span = xml.add("a");
                span.setHref(link);
            }
        }
    }
}
