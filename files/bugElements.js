function getModel(parent, data) {
    if (data.type == 'BugExpandableEntry') return new BugExpandableEntry(parent, data);
    if (data.type == 'BugList') return new BugList(parent, data);
    if (data.type == 'BugInlineList') return new BugInlineList(parent, data);
    if (data.type == 'BugCallable') return new BugCallable(parent, data);
    if (data.type == 'BugText') return new BugText(parent, data);
    if (data.type == 'BugLink') return new BugLink(parent, data);
}


class BugElement {
    constructor(parent, data, view) {
        this.parent = parent;
        this.data = data;
        this.view = view;
        if (this.view != null) {
            if (data.clazz != null) this.view.addClass(data.clazz);
        }
    }
}

class BugGroup extends BugElement {
    constructor(parent, data, view) {
        super(parent, data, view)
        this.elements = [];
    }

    extractElements(parentView, elementsData, elements) {
        for (var i = 0; i < elementsData.length; i++) {
            loadContent(this, elementsData[i], function(model) {
                parentView.append(model.view);
                elements.push(model);
            }.bind(this))
        }
    }
}

class BugList extends BugGroup {
    constructor(parent, data) {
        super(parent, data, $('<div class="bugList">'))
        this.extractElements(this.view, data.elements, this.elements);
    }
}

class BugInlineList extends BugGroup {
    constructor(parent, data) {
        super(parent, data, $('<span class="BugInlineList">'))
        this.extractElements(this.view, data.elements, this.elements);
    }
}

class BugText extends BugElement {
    constructor(parent, data, view) {
        if (view == null) view = $('<span>');
        super(parent, data, view)
        if (data.text != null) this.view.append(data.text);
        if (data.tooltip != null) this.view.attr('title', data.tooltip);
    }
}

class BugLink extends BugText {
    constructor(parent, data) {
        super(parent, data, $('<a>'))
        this.view.attr('target', '_blank');
        this.view.attr('href', data.url);
    }
}

class BugCallable extends BugElement {
    constructor(parent, data) {
        super(parent, data, $('<span class="callable"></span>'))
        var invoke = () => { this.invoke() };
        var reset = () => { this.parent.refreshElements(); };
        if (data.parentheses == true) this.view.append("("); else this.view.append(" ");
        this.data.parameters.forEach(function (parameter, index) {
            if (index != 0) this.view.append(", ");
            var val = parameter.value == undefined ? "" : parameter.value
            parameter.view = $('<span class="parameter"></span>');
            if (parameter.clazz != null) parameter.view.append($('<span class="clazz">' + parameter.clazz + '</span>'));
            if (parameter.name != null) parameter.view.append($('<span class="name">' + parameter.name + '</span>'));
            parameter.editView = $('<span class="editable">' + val + '</span>');
            makeEditable(parameter.editView, invoke, reset);
            parameter.view.append(parameter.editView);
            if (parameter.unit != null) parameter.view.append(parameter.unit);
            this.view.append(parameter.view);
        }, this);
        if (data.parentheses == true) {
            this.view.append(")");
            this.invokeView = $('<span class="invoke">\u2607</span>');
            this.invokeView.click(invoke);
            this.view.append(this.invokeView);
        }
    }

    invoke() {
        var callable = this;
        var request = {};
        this.data.parameters.forEach(function (parameter, index) {
            request[parameter.id] = parameter.editView.text();
        });
        $.ajax({
            type: "GET",
            url: this.data.url,
            data: request,
            success: function(result) {
                if (callable.data.action == "expandResult") {
                    callable.parent.setExpanded(getModel(callable, result).view);
                } else if (callable.data.action == "refreshElements") {
                    callable.parent.refreshElements();
                }
            },
            error: function(result) {
                if (callable.data.action == "expandResult") {
                    callable.parent.setExpanded($('<span class="error">' + getError(result) + '</span>'));
                } else if (callable.data.action == "refreshCallables") {
                        alert(getError(result));
                }
             },
        });
    }

    refresh() {
        this.data.parameters.forEach(function (parameter, index) {
            if (parameter.refresh != null) {
                $.ajax({
                    type: "GET",
                    url: parameter.refresh,
                    success: function(result) {
                        parameter.editView.text(result);
                    },
                    error: function(result) {
                        alert(getError(result));
                     },
                });
            }
        });
    }
}

class BugExpandableEntry extends BugGroup {
    constructor(parent, data) {
        super(parent, data, $('<div class="bugEntry">'))
        var bugEntry = this;
        this.data = data;
        this.titleView = getModel(this, data.title).view;
        this.titleView.addClass("title");
        this.titleView.click(bugEntry.toggleExpand.bind(this));
        this.view.append(this.titleView);
        if (data.expand != null) { this.titleView.addClass("closed") }
        this.extractElements(this.view, data.elements, this.elements);
    }

    getContentView() {
        if (this.contentView == null) {
            this.contentView = $('<div class="entryContent">');
            this.view.append(this.contentView);
        }
        return this.contentView;
    }

    toggleExpand() {
        if (this.expanderContentView == null) {
            loadContent(this, this.data.expand, function(content) {
                this.expanderContentView = content.view;
                this.getContentView().append(this.expanderContentView);
                this.titleView.toggleClass("opened")
                this.titleView.toggleClass("closed")
            }.bind(this))
        } else {
            this.expanderContentView.toggle()
            this.titleView.toggleClass("opened")
            this.titleView.toggleClass("closed")
        }
    }

    setExpanded(view) {
        this.getContentView().empty();
        this.expanderContentView = view;
        this.getContentView().append(this.expanderContentView);
        this.titleView.addClass("opened")
        this.titleView.removeClass("closed")
    }

    refreshElements() {
        this.elements.forEach(function (element) {
            if (element.refresh != null) {
                element.refresh();
            }
        }, this);
    }
}
