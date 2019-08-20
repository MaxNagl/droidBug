function getModel(parent, data) {
    if (data.type == 'BugExpandableEntry') return new BugExpandableEntry(parent, data);
    if (data.type == 'BugList') return new BugList(parent, data);
    if (data.type == 'BugInlineList') return new BugInlineList(parent, data);
    if (data.type == 'BugInvokable') return new BugInvokable(parent, data);
    if (data.type == 'BugText') return new BugText(parent, data);
    if (data.type == 'BugLink') return new BugLink(parent, data);
    if (data.type == 'BugInput') return new BugInput(parent, data);
}


class BugElement {
    constructor(parent, data, view) {
        this.parent = parent;
        this.data = data;
        this.view = view;
        if (this.view != null) {
            if (data.clazz != null) this.view.addClass(data.clazz);
            if (data.onClick != null) {
                this.view.click(function () {
                    if (data.onClick == "invoke") this.getParent(BugInvokable).invoke();
                }.bind(this));
                this.view.addClass("clickable");
            }
        }
    }

    getParent(clazz) {
        return clazz == null || this.parent instanceof clazz ? this.parent : this.parent.getParent(clazz);
    }

    refresh() {
        if (this.data.refreshUrl != null) {
            $.ajax({
                type: "GET",
                url: this.data.refreshUrl,
                success: function (result) {
                    this.setValue(result);
                }.bind(this),
                error: function (result) {
                    alert(getError(result));
                }.bind(this),
            });
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
            loadContent(this, elementsData[i], function (model) {
                parentView.append(model.view);
                elements.push(model);
            }.bind(this))
        }
    }

    refresh() {
        this.elements.forEach(function (element) {
            if (element.refresh != null) {
                element.refresh();
            }
        }, this);
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

    setValue(value) {
        if (this.view.text() != value) {
            this.view.text(value)
        }
    }

    getValue() {
        return this.view.text()
    }
}

class BugLink extends BugText {
    constructor(parent, data) {
        super(parent, data, $('<a>'))
        this.view.attr('target', '_blank');
        this.view.attr('href', data.url);
    }
}

class BugInput extends BugText {
    constructor(parent, data) {
        super(parent, data, $('<span>'))
        this.view.addClass('editable');
        this.view.attr('contentEditable', 'true');
        this.view.on('keydown', function (e) {
            if (e.keyCode == 13) {
                e.preventDefault();
                this.onEnter();
            }
            if (e.keyCode == 27) {
                e.preventDefault();
                this.onEsc();
            }
        }.bind(this));
    }

    onEnter() {
        this.getParent(BugInvokable).invoke();
    }

    onEsc() {
        console.log("onEsc");
    }
}

class BugInvokable extends BugGroup {
    constructor(parent, data) {
        super(parent, data, $('<span class="callable">'))
        this.extractElements(this.view, data.elements, this.elements);
    }

    invoke() {
        var request = {};
        this.addElementsToRequest(request, this.elements);
        this.elements.forEach(function (element) {
            if (element.data != null) {
                if (element.data.callId != null) {
                    request[element.data.callId] = element.getValue();
                }
            }
        });
        $.ajax({
            type: "GET",
            url: this.data.url,
            data: request,
            success: function (result) {
                if (this.data.action == "expandResult") {
                    this.getParent(BugExpandableEntry).setExpanded(getModel(this, result).view);
                } else if (this.data.action == "refreshEntry") {
                    this.getParent(BugExpandableEntry).refresh();
                }
            }.bind(this),
            error: function (result) {
                if (this.data.action == "expandResult") {
                    this.getParent(BugExpandableEntry).setExpanded($('<span class="error">' + getError(result) + '</span>'));
                } else if (callable.data.action == "refreshEntry") {
                    alert(getError(result));
                }
            }.bind(this),
        });
    }

    addElementsToRequest(request, elements) {
        elements.forEach(function (element) {
            if (element.data != null && element.data.callId != null) request[element.data.callId] = element.getValue();
            if (element.elements != null) this.addElementsToRequest(request, element.elements)
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
        if (data.expand != null) {
            this.view.addClass("closed")
        }
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
            loadContent(this, this.data.expand, function (content) {
                this.expanderContentView = content.view;
                this.getContentView().append(this.expanderContentView);
                this.view.toggleClass("opened")
                this.view.toggleClass("closed")
            }.bind(this))
        } else {
            this.expanderContentView.toggle()
            this.view.toggleClass("opened")
            this.view.toggleClass("closed")
        }
    }

    setExpanded(view) {
        this.getContentView().empty();
        this.expanderContentView = view;
        this.getContentView().append(this.expanderContentView);
        this.view.addClass("opened")
        this.view.removeClass("closed")
    }
}
