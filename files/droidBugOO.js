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

$(function () {
    $.getJSON('tabs.json', function(tabsJson) {
        new Tabs(tabsJson);
        $("#loading").remove()
    });
});