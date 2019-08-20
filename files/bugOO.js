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
        loadContent(this, this.data.content, function(content) {
            this.contentView.append(content.view)
        }.bind(this));
    }
}

function loadContent(parent, content, callback) {
    if (typeof content === 'string' || content instanceof String) {
        $.getJSON(content, function(data) {
            callback(getModel(parent, data))
        });
    } else {
        callback(getModel(parent, content))
    }
}

function getError(result) {
    if (result.responseText != undefined) {
        return result.responseText;
    } else {
        return 'HTTP Error: ' + result.status + " " + result.statusText;
    }
}

$(function () {
    $.getJSON('tabs.json', function(tabsJson) {
        new Tabs(tabsJson);
        $("#loading").remove()
    });
});