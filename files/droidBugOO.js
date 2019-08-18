class Tabs {
    constructor(json) {
        this.listView = $('<div id="tabs">');
        this.contentsView = $('<div id="tabsContents">');
        $("body").append(this.listView);
        $("body").append(this.contentsView);

        this.tabs = [];
        json.forEach(function (tabJson) {
            this.tabs.push(new Tab(this, tabJson))
        }, this);

        this.showTab(this.tabs[0]);
    }

    showTab(tab) {
        for (var i = 0; i < this.tabs.length; i++) {
            if (this.tabs[i] == tab) {
                this.tabs[i].show();
            } else {
                this.tabs[i].hide();
            }
        }
    }
}

class Tab {
    constructor(tabs, data) {
        this.data = data
        this.tabView = $('<span class="tab">' + data.title + '</span>');
        this.contentView = $('<div>');
        tabs.listView.append(this.tabView);
        tabs.contentsView.append(this.contentView);

        this.tabView.click(() => {
            tabs.showTab(this)
        });

        this.load();
    }

    show() {
        this.contentView.show();
        this.tabView.addClass("active");
    }

    hide() {
        this.contentView.hide();
        this.tabView.removeClass("active");
    }

    load() {
        loadContent(this.data.content, function(content) {
            this.contentView.append(content.view)
        }.bind(this));
    }
}

class BugObject {
    constructor(data) {
        var bugObject = this;
        this.data = data;
        this.view = $('<div class="bugObject">');
        this.titleView = $('<span class="title">' + data.name + '</span>');
        this.view.append(this.titleView);
        if (data.expand != null) {
            this.titleView.addClass("closed")
            this.titleView.click(bugObject.toggleExpand.bind(this));
        }
        if (data.clazz != null) {
            var clazzView = $('<span class="clazz">' + data.clazz + '</span>');
            this.titleView.prepend(clazzView);
        }
        if (data.modifiers != null) {
            var valView = $('<span class="modifier">' + data.modifiers + '</span>');
            this.titleView.prepend(valView);
        }
        if (data.callable != null) {
            this.view.append(new Callable(data.callable, this).view);
        }
        if (data.value != null) {
            var valView = $('<span class="value">' + data.value + '</span>');
            this.view.append(": ");
            this.view.append(valView);
        }
        if (data.properties != null) {
            data.properties.forEach(function (property) {
                var pView = $('<span title="' + property.name + '" class="property">' + property.value + '</span>');
                this.view.append(pView);
            }, this);
        }
        if (data.actions != null) {
            data.actions.forEach(function (action) {
                var pView = $('<a class="action">[' + action.name + ']</a>');
                if (action.action == 'linkNewWindow') {
                    pView.attr('target', '_blank');
                    pView.attr('href', action.value);
                } else if (action.action == 'link') {
                    pView.attr('href', action.value);
                }
                this.view.append(pView);
            }, this);
        }
    }

    getContentView() {
        if (this.contentView == null) {
            this.contentView = $('<div class="objectContent">');
            this.view.append(this.contentView);
        }
        return this.contentView;
    }

    toggleExpand() {
        if (this.expanderContentView == null) {
            loadContent(this.data.expand, function(content) {
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
}

class Callable {
    constructor(data, bugObject) {
        var invoke = () => { this.onClick() };
        this.bugObject = bugObject;
        this.data = data;
        this.view = $('<span class="callable"></span>');
        this.view.append("(");
        this.data.parameters.forEach(function (parameter, index) {
            if (index != 0) this.view.append(", ");
            var val = parameter.value == undefined ? "" : parameter.value
            parameter.view = $('<span class="parameter"><span class="clazz">' + parameter.clazz + '</span></span>');
            if (parameter.name != null) parameter.view.append($('<span class="name">' + parameter.name + '</span>'));
            parameter.editView = $('<span class="editable">' + val + '</span>');
            makeEditable(parameter.editView, invoke);
            parameter.view.append(parameter.editView);
            this.view.append(parameter.view);
        }, this);
        this.view.append(")");
        this.invokeView = $('<span class="invoke">\u2607</span>');
        this.invokeView.click(invoke);
        this.view.append(this.invokeView);
    }

    onClick() {
        var bugObject = this.bugObject;
        var request = {};
        this.data.parameters.forEach(function (parameter, index) {
            request[parameter.id] = parameter.editView.text();
        });
        $.ajax({
            type: "GET",
            url: this.data.url,
            data: request,
            success: function(result) {
                bugObject.setExpanded(loadContentData(result).view);
            },
            error: function(result) {
                bugObject.setExpanded($('<span class="error">' + getError(result) + '</span>'));
             },
        });
    }
}

class BugList {
    constructor(data) {
        var bugObject = this;
        this.data = data;
        this.view = $('<div class="bugList">');
        for (var i = 0; i < data.elements.length; i++) {
            loadContent(data.elements[i], function(content) {
                this.view.append(content.view);
            }.bind(this))
        }
    }
}

function loadContent(content, callback) {
    if (typeof content === 'string' || content instanceof String) {
        $.getJSON(content, function(data) {
            callback(loadContentData(data))
        });
    } else {
        callback(loadContentData(content))
    }
}

function loadContentData(data) {
    var val;
    if (data.type == 'object') val = new BugObject(data);
    if (data.type == 'list') val = new BugList(data);
    return val;
}

function getError(result) {
    if (result.responseText != undefined) {
        return result.responseText;
    } else {
        return 'HTTP Error: ' + result.status + " " + result.statusText;
    }
}

function makeEditable(view, invoke) {
    view.attr('contentEditable', 'true');
    view.on('keydown', function(e) {
        if(e.keyCode == 13) {
            e.preventDefault();
            invoke();
        }
   });
}

$(function () {
    $.getJSON('tabs.json', function(tabsJson) {
        new Tabs(tabsJson);
        $("#loading").remove()
    });
});