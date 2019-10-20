$.fn.loadContent = function (content, mimeType, append) {
    if (append != true) this.empty();
    if (mimeType == 'application/json') {
        if ((typeof content) == 'string') content = JSON.parse(content);
        if (Array.isArray(content)) {
            content.forEach(function(entry) {
                loadModel(this.data('model'), entry, function (model) {
                    this.append(model.view);
                }.bind(this));
            }.bind(this));
        } else {
            loadModel(this.data('model'), content, function (model) {
                this.append(model.view);
            }.bind(this));
        }
    } else if (mimeType == 'text/plain') {
        this.append(content);
    }
    return this;
}

function loadModel(parent, content, callback) {
    if (content.type == "BugInclude") {
        $.ajax({
            type: "GET",
            url: content.url,
            success: function (result) {
                var model = getModel(parent, result);
                callback(model);
                if (model.view != null) viewAdded(model.view);
            }.bind(this),
            error: function (result) {
                alert(getError(result));
            }.bind(this),
        });
    } else {
        var model = getModel(parent, content);
        callback(model);
        if (model.view != null) viewAdded(model.view);
    }
}

function getModel(parent, data) {
    if (data.type == 'BugInclude') return new BugInclude(parent, data);
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
            if (this.data.reference != null) {
                this.view.attr({ draggable: "true" });
                this.view.on('dragstart', function (e) {
                    if (!e.shiftKey && !e.altKey && !e.ctrlKey && !e.metaKey) {
                        e.originalEvent.dataTransfer.setData("ref", this.data.reference);
                        event.stopPropagation();
                    }
                }.bind(this));
            }
            if (data.stream != null) {
                this.appendStream();
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
                success: function (result, textStatus) {
                    this.setValue(result);
                    if (this.setMode != null) this.setMode(textStatus == 'nocontent' ? 'null' : 'text');
                }.bind(this),
                error: function (result) {
                    alert(getError(result));
                }.bind(this),
            });
        }
    }

    appendStream(param) {
            $.ajax({
            type: "GET",
            url: this.data.stream + (param == null ? "" : param),
            success: function (result, status, xhr) {
                var scrolledDown = this.view.scrollTop() + this.view.innerHeight() >= this.view[0].scrollHeight;
                this.view.loadContent(result, xhr.getResponseHeader("Content-Type"), true);
                if (document.contains(this.view[0])) this.appendStream("?index=" + xhr.getResponseHeader("next") + "&uid=" + xhr.getResponseHeader("uid"));
                if (scrolledDown) this.view.scrollTop(this.view[0].scrollHeight);
            }.bind(this),
            timeout: 60000,
            error: function (result) {
                if ((result.status < 400 || result.status > 499) && document.contains(this.view[0])) this.appendStream(param);
            }.bind(this)
        });
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
        this.view.bind('load', autoScale);
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
        if (data.mode == null) this.toggleMode(); else this.setMode(data.mode);
        if (data.enabled == false) {
            this.view.prop('disabled', true);
        } else {
            this.view.attr('contenteditable', true);
            this.view.on('keydown', function (e) {
                if (e.keyCode == 13) { // enter
                    if (!e.shiftKey && (data.multiline != true || e.ctrlKey)) {
                        e.preventDefault();
                        this.onEnter();
                    }
                }
                if (e.keyCode == 27) { // esc
                    e.preventDefault();
                    this.onEsc();
                }
                if (e.keyCode == 8 || e.keyCode == 46) { // backspace or delete
                    if (this.data.nullable == true && this.view.text() == "") {
                        if (this.mode == 'null') this.unsetNull(); else this.setMode('null');
                    }
                }
            }.bind(this));
            this.view.on('keypress', function (e) {
                if (e.keyCode == 32 && (e.shiftKey || e.altKey || e.ctrlKey || e.metaKey)) { // ctrl
                    e.preventDefault();
                    this.toggleMode();
                }
            }.bind(this));
            this.view.on('paste', function (e) {
                e.preventDefault();
                document.execCommand("insertText", false, e.originalEvent.clipboardData.getData("text/plain"));
            }.bind(this));
            this.view.on('keyup keypress focus blur change', function (e) {
                if (this.mode == 'null' && this.view.text() != '') this.unsetNull()
            }.bind(this));
            this.view.on('dragover', function (e) {
                setCaretToCoordinated(e.clientX, e.clientY);
                e.preventDefault();
            }.bind(this));
            this.view.on('drop', function (e) {
                var ref = e.originalEvent.dataTransfer.getData("ref");
                if (ref != null && ref.length > 0 && this.data.referenceable) {
                    if (this.mode.startsWith('script-')) {
                        var range = setCaretToCoordinated(e.clientX, e.clientY);
                        if (range != null) {
                            range.insertNode(document.createTextNode(ref));
                        } else {
                            this.view.append(ref);
                        }
                    } else {
                        this.setMode("ref");
                        this.setValue(ref);
                    }
                }
                e.preventDefault();
            }.bind(this));
        }
    }

    setMode(mode) {
        if (mode == 'script') mode = availableScripts.empty ? "" : availableScripts[0];
        if (mode == "null" && this.mode != "null") this.lastMode = this.mode;
        this.mode = mode;
        this.view.attr('mode', mode);
        var prefix = null;
        if (mode.startsWith('script-')) {
            this.view.attr('mode', 'script');
            prefix = mode.substring(7);
        }
        if (mode == 'null') prefix = 'null';
        if (mode == 'ref') prefix = '@';
        this.view.attr('prefix', prefix);
        if (prefix != null) {
            this.view.css('padding-left', getTextWidth(prefix) + 12);
        } else {
            this.view.css('padding-left', '');
        }
    }

    setValue(value) {
        super.setValue(value)
        if (value == null) {
            this.setMode("null");
        } else if (this.mode == "null") {
            this.toggleMode();
        }
    }

    getValueType() {
        return this.mode;
    }

    unsetNull() {
        if (this.lastMode != null) this.setMode(this.lastMode); else this.toggleMode();
    }

    getAllModes() {
        var allModes = [];
        if (this.data.textable == true) allModes.push('text');
        if (this.data.scriptable == true) allModes = allModes.concat(availableScripts);
        if (this.data.referenceable == true) allModes.push('ref');
        if (this.data.nullable == true) allModes.push('null');
        return allModes;
    }

    toggleMode() {
        var allModes = this.getAllModes()
        var index = allModes.indexOf(this.mode);
        var nextIndex = (index + 1) % allModes.length;
        if (allModes[nextIndex] == 'null' && this.getValue() != '') nextIndex = (index + 2) % allModes.length;
        this.setMode(allModes[nextIndex]);
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
            type: "POST",
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
                var type = element.getValueType == null ? null : element.getValueType();
                if (value != null) request[element.data.callId] = value;
                if (type != null) request[element.data.callId + '-type'] = type;
            }
            if (element.elements != null) this.addElementsToRequest(request, element.elements)
        }.bind(this));
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
        if (data.orientation == 'horizontal') this.view.css({ 'flex-direction': 'row', 'height': '100%' });
        this.extractElements(this.view, data.elements, this.elements);
        var last;
        this.elements.forEach(function(model) {
            if (model.data.splitType == 'resizeHandle') {
                makeResizable(last.view, model.view, data.orientation == 'vertical');
                model.view.addClass(data.orientation);
            }
            last = model;
        })
    }
}

class BugSplitElement extends BugElement {
    constructor(parent, data) {
        super(parent, data, $('<div>'));
        if (data.weight != null) this.view.css('flex-grow', data.weight);
        if (data.fixed != null) this.view.css('flex-basis', data.fixed);
        if (data.content != null) {
            loadModel(this, data.content, function (model) {
                this.view.append(model.view);
            }.bind(this))
        }
    }
}

function makeResizable(element, handle, resizeHeight) {
	handle.bind('mousedown.resize', function(e) {
		var start = { x: e.clientX, y: e.clientY, width: parseInt(element.width(), 10), height: parseInt(element.height(), 10) };
		$(document).bind('mousemove.resize', function(e) {
            element.css('flex-grow', '0')
            element.css('flex-basis', resizeHeight ? (start.height + e.clientY - start.y) : (start.width + e.clientX - start.x) + 'px')
        });
		$(document).bind('mouseup.resize', function(e) {
            $(document).unbind('mousemove.resize mouseup.resize selectstart.resize');
        });
		$(document).bind('selectstart.resize', function(e) {
            e.stopPropagation();
            e.preventDefault();
        });
	});
};

var textWidthCache = {}
function getTextWidth(text) {
    if (textWidthCache[text] == null) {
        var measure = $('<span class="measurePre">').append(text);
        $('html').append(measure);
        textWidthCache[text] = measure.width();
        measure.remove();
    }
    return textWidthCache[text];
}

function viewAdded(view) {
    if (document.contains(view[0])) {
        autoScale(null, view.parent());
        rotate3d(null, view.parent());
    }
}

function rotate3d(e, parent) {
    $('.root3d', parent).each(function () {
        var v = $(this);
        if (v.data('root3d') != null) return;
        var data = { rotateY: 45, rotateX: 0, translateZ: 100};
        v.data('root3d', data);
        var p = v.parent().parent();
        p.bind('mousedown.rotate3d', function(e) {
            var x = e.pageX, y = e.pageY;
            $(document).bind('mousemove.rotate3d', function(e) {
                if (e.shiftKey) {
                    data.rotateY += e.pageX - x;
                    data.rotateX -= e.pageY - y;
                    v.css("transform", "rotateY(" + data.rotateY + "deg) rotateX(" + data.rotateX + "deg)");
                }
                x = e.pageX;
                y = e.pageY;
            });
            $(document).bind('mouseup.rotate3d', function(e) {
                $(document).unbind('mousemove.rotate3d mouseup.rotate3d selectstart.rotate3d');
            });
            $(document).bind('selectstart.rotate3d', function(e) {
                e.stopPropagation();
                e.preventDefault();
            });
        });
        p.bind('mousewheel', function(e) {
            if (e.shiftKey) {
                var sign = Math.sign(data.translateZ);
                data.translateZ += e.originalEvent.wheelDelta / 10;
                if (sign != 0 && sign != Math.sign(data.translateZ)) data.translateZ = 0;
                $('.layer3d', v).each(function() {
                    $(this).css('transform', 'translateZ(' + data.translateZ + 'px)');
                });
            }
        });
    });
}

function autoScale(event, parent) {
    if (parent == null) parent = $('body');
    $('.autoScale', parent).each(function () {
        var v = $(this);
        if (v.data('autoscale') != null) {
            if (v.data('reset') != null) v.data('reset')();
            return;
        }
        var p = v.parent();
        var data = { rotateY: 45, translateY: 0, scale: 1};
        var update = function() {
            v.css("transform", "scale(" + data.scale + "," + data.scale + ") translate(" + data.translateX + "px, " + data.translateY +"px)");
        };
        var reset = function() {
            v.css({'width': '', 'height': '', 'transform': ''});
            var h = v.outerHeight(), w = v.outerWidth();
            var ph = v.parent().height(), pw = v.parent().width();
            data.scale = Math.min(1, Math.min(ph / h, pw / w));
            if (v.hasClass('autoScaleCenter')) {
                data.translateX = (pw / data.scale - w) / 2;
                data.translateY = (ph / data.scale - h) / 2;
            }
            update();
            v.css('width', pw);
            v.css('height', ph);
        };
        reset();
        p.css('overflow', 'hidden');
        p.bind('mousedown.autoscale', function(e) {
            var x = e.pageX, y = e.pageY;
            $(document).bind('mousemove.autoscale', function(e) {
                if (e.buttons != 0) {
                    if (!e.shiftKey) {
                        data.translateX += (e.pageX - x) / data.scale;
                        data.translateY += (e.pageY - y) / data.scale;
                        v.data('reset', null);
                        update();
                        e.stopPropagation();
                    }
                }
                x = e.pageX;
                y = e.pageY;
            });
            $(document).bind('mouseup.autoscale', function(e) {
                $(document).unbind('mousemove.autoscale mouseup.autoscale selectstart.autoscale');
            });
            $(document).bind('selectstart.autoscale', function(e) {
                e.stopPropagation();
                e.preventDefault();
            });
        });
        p.bind('mousewheel', function(e) {
            if (!e.shiftKey) {
                var oldScale = data.scale;
                data.scale = data.scale * (1000 + e.originalEvent.wheelDelta) / 1000;
                var x = e.pageX - p.offset().left;
                var y = e.pageY - p.offset().top;
                data.translateY -= y / oldScale - y / data.scale;
                data.translateX -= x / oldScale - x / data.scale;
                console.log(e);
                v.data('reset', null);
                update();
                e.stopPropagation();
            }
        });
        v.data('autoscale', data);
        v.data('reset', reset);
    });
}

function setCaretToCoordinated(x, y) {
    var range = null;
    if (document.caretPositionFromPoint) {
        var pos = document.caretPositionFromPoint(x, y);
        range = document.createRange();
        range.setStart(pos.offsetNode, pos.offset);
        range.collapse();
    } else if (document.caretRangeFromPoint) {
        range = document.caretRangeFromPoint(x, y);
    }
    if (range != null) {
        window.getSelection().removeAllRanges();
        window.getSelection().addRange(range);
    }
    return range;
}

$(window).resize(autoScale);