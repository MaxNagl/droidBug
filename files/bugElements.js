$.fn.loadBugElement = function load(content, append) {
    if (append != true) this.empty();
    loadModel(this.data('model'), content, function (model) {
        this.append(model.view);
    }.bind(this));
    return this;
}

function loadModel(parent, content, callback) {
    if (content.type == "BugInclude") content = content.url
    if (typeof content === 'string' || content instanceof String) {
        $.ajax({
            type: "GET",
            url: content,
            success: function (result) {
                callback(getModel(parent, result))
            }.bind(this),
            error: function (result) {
                alert(getError(result));
            }.bind(this),
        });
    } else {
        callback(getModel(parent, content))
    }
}

function getModel(parent, data) {
    if (data.type == 'BugEntry') return new BugEntry(parent, data);
    if (data.type == 'BugList') return new BugList(parent, data);
    if (data.type == 'BugDiv') return new BugDiv(parent, data);
    if (data.type == 'BugInlineList') return new BugInlineList(parent, data);
    if (data.type == 'BugInvokable') return new BugInvokable(parent, data);
    if (data.type == 'BugText') return new BugText(parent, data);
    if (data.type == 'BugLink') return new BugLink(parent, data);
    if (data.type == 'BugPre') return new BugPre(parent, data);
    if (data.type == 'BugImg') return new BugImg(parent, data);
    if (data.type == 'BugInputText') return new BugInputText(parent, data);
    if (data.type == 'BugInputList') return new BugInputList(parent, data);
    if (data.type == 'BugInputCheckbox') return new BugInputCheckbox(parent, data);
    if (data.type == 'BugTabs') return new BugTabs(parent, data);
    if (data.type == 'BugSplit') return new BugSplit(parent, data);
    if (data.type == 'BugSplitElement') return new BugSplitElement(parent, data);
}

function getError(result) {
    if (result.responseText != undefined) {
        return result.responseText;
    } else {
        return 'HTTP Error: ' + result.status + " " + result.statusText;
    }
}

class BugElement {
    constructor(parent, data, view) {
        this.parent = parent;
        this.data = data;
        this.view = view;
        if (this.view != null) {
            this.view.data('model', this);
            this.view.addClass(this.constructor.name);
            if (data.clazz != null) this.view.addClass(data.clazz);
            if (data.id != null) this.view.attr("id", data.id);
            if (data.onClick != null) {
                this.view.click(function () {
                    if (data.onClick == "invoke") this.getParent(BugInvokable).invoke(this);
                    else if (data.onClick == "expand") this.getParent(BugEntry).toggleExpand();
                    else eval(data.onClick);
                    event.stopPropagation();
                }.bind(this));
                this.view.addClass("clickable");
            }
            if (data.styles != null) {
                Object.keys(data.styles).forEach(function (style) {
                    this.view.css(style, data.styles[style]);
                }.bind(this));
            }
            if (this.data.hoverGroup != null) {
                this.view.attr("hoverGroup", this.data.hoverGroup);
                this.view.mouseover(function () {
                    $('.groupHover').removeClass("groupHover");
                    $('[hoverGroup="' + this.data.hoverGroup + '"]').addClass("groupHover");
                    event.stopPropagation();
                }.bind(this));
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
            loadModel(this, elementsData[i], function (model) {
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
        super(parent, data, $('<div>'))
        this.extractElements(this.view, data.elements, this.elements);
    }
}

class BugDiv extends BugGroup {
    constructor(parent, data) {
        super(parent, data, $('<div>'))
        this.extractElements(this.view, data.elements, this.elements);
    }
}

class BugImg extends BugElement {
    constructor(parent, data) {
        super(parent, data, $('<img>'))
        if (data.src != null) this.view.attr("src", data.src);
    }
}

class BugInlineList extends BugGroup {
    constructor(parent, data) {
        super(parent, data, $('<span>'))
        this.extractElements(this.view, data.elements, this.elements);
    }
}

class BugText extends BugElement {
    constructor(parent, data, view) {
        if (view == null) view = $('<span>');
        super(parent, data, view)
        this.setValue(data.text);
        if (data.tooltip != null) this.view.attr('title', data.tooltip);
    }

    setValue(value) {
        if (this.view.text() != value) {
            this.view.text(value)
            if (value == null) {
                this.view.addClass("null");
            } else {
                this.view.removeClass("null");
            }
        }
    }

    getValue() {
        if (this.view.hasClass("null")) return null;
        return this.view.text()
    }
}

class BugPre extends BugText {
    constructor(parent, data) {
        super(parent, data, $('<pre>'))
    }
}

class BugLink extends BugText {
    constructor(parent, data) {
        super(parent, data, $('<a>'))
        this.view.attr('target', '_blank');
        this.view.attr('href', data.url);
    }
}

class BugInputText extends BugText {
    constructor(parent, data) {
        super(parent, data, $('<span>'));
        if (data.enabled == false) {
            this.view.prop('disabled', true);
        } else {
            this.view.attr('contenteditable', true);
            this.view.on('keydown', function (e) {
                if (e.keyCode == 13) { // enter
                    e.preventDefault();
                    this.onEnter();
                }
                if (e.keyCode == 27) { // esc
                    e.preventDefault();
                    this.onEsc();
                }
                if (e.keyCode == 8 || e.keyCode == 46) { // backspace or delete
                    if (this.data.nullable == true && this.view.text() == "") this.view.toggleClass("null");
                }
            }.bind(this));
            this.view.on('keyup keypress focus blur change', function (e) {
                if (this.view.text() != "") this.view.removeClass("null");
            }.bind(this));
        }
    }

    onEnter() {
        this.getParent(BugInvokable).invoke(this);
    }

    onEsc() {
        console.log("onEsc");
    }
}

class BugInputList extends BugElement {
    constructor(parent, data) {
        super(parent, data, $('<select>'));
        data.options.forEach(function (option) {
            this.view.append($('<option value="' + option.id + '">' + option.text + '</option>'));
        }.bind(this));
        this.view.change(function () {
            if (data.onChange != null) {
                eval(data.onChange);
            } else {
                this.getParent(BugInvokable).invoke(this);
            }
        }.bind(this));
        this.view.val(data.text);
    }

    setValue(value) {
        this.view.val(value);
    }

    getValue() {
        return this.view.val();
    }
}

class BugInputCheckbox extends BugElement {
    constructor(parent, data) {
        super(parent, data, $('<label>'));
        this.checkboxView = $('<input type="checkbox">');
        this.checkboxView.attr('checked', data.checked);
        this.view.append(this.checkboxView);
        if (data.text != null) this.view.append($('<span>' + data.text + '</span>'));
        this.view.change(function () {
            if (data.onChange != null) {
                eval(data.onChange);
            } else {
                this.getParent(BugInvokable).invoke(this);
            }
        }.bind(this));
        this.view.val(data.text);
    }

    setValue(value) {
        this.view.val(value);
    }

    getValue() {
        return this.view.val();
    }
}

class BugInvokable extends BugGroup {
    constructor(parent, data) {
        super(parent, data, $('<span>'))
        this.extractElements(this.view, data.elements, this.elements);
    }

    invoke(source) {
        var request = {};
        this.addElementsToRequest(request, this.elements);
        $.ajax({
            type: "GET",
            url: this.data.url,
            data: request,
            success: function (result, status) {
                if (status == "nocontent") result = null;
                if (this.data.action == "expandResult") {
                    loadModel(this, result, function (model) {
                        this.getParent(BugEntry).setExpanded(model.view);
                    }.bind(this))
                } else if (this.data.action == "setValue") {
                    source.setValue(result);
                } else if (this.data.action == "refreshEntry") {
                    this.getParent(BugEntry).refresh();
                }
            }.bind(this),
            error: function (result) {
                if (this.data.action == "expandResult") {
                    this.getParent(BugEntry).setExpanded($('<span class="error">' + getError(result) + '</span>'));
                } else {
                    alert(getError(result));
                }
            }.bind(this),
        });
    }

    addElementsToRequest(request, elements) {
        elements.forEach(function (element) {
            if (element.data != null && element.data.callId != null) {
                var value = element.getValue();
                if (value != null) request[element.data.callId] = value;
            }
            if (element.elements != null) this.addElementsToRequest(request, element.elements)
        });
    }
}

class BugEntry extends BugGroup {
    constructor(parent, data) {
        super(parent, data, $('<div>'));
        var bugEntry = this;
        this.data = data;
        this.opener = $('<span class="opener">');
        this.view.append(this.opener);
        this.opener.click(this.toggleExpand.bind(this));
        this.extractElements(this.view, data.elements, this.elements);
        if (data.expand != null) {
            this.view.addClass("closed")
            if (data.autoExpand == true) {
                this.toggleExpand();
            }
        }
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
            if (this.data.expand == null) return;
            loadModel(this, this.data.expand, function (content) {
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

class BugTabs extends BugElement {
    constructor(parent, data) {
        super(parent, data, $('<div>'));
        this.tabsView = $('<div class="tabBar">');
        this.contentsView = $('<div class="tabsContents">');
        this.view.append(this.tabsView);
        this.view.append(this.contentsView);

        var select = null;
        this.tabs = [];
        data.tabs.forEach(function (tab) {
            tab.tabView = $('<span class="tab">' + tab.title + '</span>');
            tab.contentView = $('<div class="tabsContentHolder">');
            var t = tab;
            tab.tabView.click(function () {
                this.showTab(t)
            }.bind(this));
            this.tabsView.append(tab.tabView);
            this.contentsView.append(tab.contentView);
            this.tabs.push(tab);
            if (select == null || tab.default == true) select = tab;
        }, this);

        if (select != null) this.showTab(select);
    }

    showTab(tab) {
        this.load(tab);
        this.tabs.forEach(function (t) {
            if (t === tab) {
                t.contentView.css('display', '');
                t.tabView.addClass('active');
            } else {
                t.contentView.css('display', 'none');
                t.tabView.removeClass('active');
            }
        }.bind(this));
    }

    load(tab) {
        if (tab.model == null) {
            loadModel(this, tab.content, function (model) {
                tab.model = model;
                tab.contentView.append(model.view);
            }.bind(this))
        }
    }
}

class BugSplit extends BugGroup {
    constructor(parent, data) {
        super(parent, data, $('<div>'));
        if (data.orientation == 'vertical') this.view.css('flex-direction', 'column');
        if (data.orientation == 'horizontal') this.view.css('flex-direction', 'row');
        this.extractElements(this.view, data.elements, this.elements);
        //data.elements.forEach(function (element) {
        //    element.contentView = $('<div class="splitElement">');
        //    this.view.append(element.contentView);
        //    loadModel(this, element.content, function (model) {
        //        element.model = model;
        //        element.contentView.append(model.view);
        //    }.bind(this))
        //}.bind(this));
    }
}

class BugSplitElement extends BugElement {
    constructor(parent, data) {
        super(parent, data, $('<div>'));
        if (data.weight != null) this.view.css('flex-grow', data.weight);
        if (data.fixed != null) this.view.css('flex-basis', data.fixed);
        loadModel(this, data.content, function (model) {
            this.view.append(model.view);
        }.bind(this))
    }
}

