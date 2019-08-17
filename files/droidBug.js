function init(tag) {
    $('[tabContent]', tag).each(function(){
        var t = $(this);
        t.unbind('click');
        $('.' + t.attr('tabContent')).hide();
        t.on('click' , function() {
            $('.active[tabContent]', t.parent()).each(function() {
                $(this).removeClass('active');
                $('.' + $(this).attr('tabContent')).hide();
            });
            openTab(t);
        });
    });
    $('[tabContent]', tag).parent().each(function(){
        var t = $(this);
        var open = null;
        $('[tabContent]', t).each(function() {
            if (open == null || containsHash($(this).attr('tabContent')))
                open = $(this);
        });
        openTab(open);
    });
    $('[editUrl]', tag).each(function(){
        var t = $(this);
        var isSelect = t.prop("tagName") == "SELECT";
        t.attr('contentEditable', 'true');
        t.unbind('keydown');
        var setValue = function (tag, data) {
            tag.attr('def', t.html());
            if (tag.prop("tagName") == "SELECT") {
                tag.val(data);
            } else {
                tag.html(data);
            }
            tag.removeClass("edited");
        }
        var success = function (data) {
            setValue(t, data);
            if (t.attr('updateAll')) {
                console.log($(t.attr('updateAll')));
                $('[updateUrl]', $(t.attr('updateAll'))).each(function() {
                    var t2 = $(this);
                    if (!t2.is(t)) {
                        $.get(t2.attr('updateUrl')).done(function(data) {
                            setValue(t2, data);
                        })
                    }
                })
                console.log();
            }
        }
        var error = function (data) {
            alert(jqXHR.responseText);
        }
        var commit = function() {
            if (isSelect && t.val() == t.attr('def')) console.log("SAME");
            if (isSelect && t.val() == t.attr('def')) return;
            var param = {};
            var paramKey = t.attr('param') === undefined ? "value" : t.attr('param');
            param[paramKey] = isSelect ? t.val() : t.text();
            $.ajax({
                type: "POST",
                url: t.attr('editUrl'),
                data: param,
                success: success,
                error: error
            });
        };
        var nullify = function() {
            $.ajax({
                type: "POST",
                url: t.attr('editUrl'),
                success: success,
                error: error
            });
        };
        if (isSelect) {
            t.change(commit);
        } else {
            makeEditable(t, commit, commit, nullify);
        }
    });
    $('input', tag).change(append);
    $('span[append]', tag).click(append);
    $('[replace]', tag).click(function(){
        var t = $(this);
        var replace = $(t.attr('replace'));
        var replaceUrl = t.attr('replaceUrl');
        loadGet(replace, replaceUrl, true, true);
    });
    $('[param]', tag).attr('contentEditable', 'true');
    $('[hoverGroup]', tag).each(function(){
        var t = $(this);
        t.mouseover(function() {
            $('.hover').removeClass("hover");
            $('[hoverGroup=' + t.attr('hoverGroup') + ']').addClass("hover");
            event.stopPropagation();
        })
    });
    $('[modTag]', tag).change(applyModTag);
    $('[modTag]', tag).trigger("change");
    autoload(tag);
    splitup();
}

function append() {
    var t = $(this);
    var appendUri = t.attr('append');
    if (appendUri === undefined) return;
    var appendId = t.attr('appendId') ? t.attr('appendId') : t.attr('id') + 'Append';
    var append = $('#' + appendId);
    var refresh = t.prop("tagName") != "INPUT";
    if(refresh || t.is(':checked')) {
        var to = $(t.attr('appendTo'));
        if (append.length == 0) {
            append = $('<div>');
            append.addClass('append');
            append.attr('id', appendId);
            to.append(append);
            refresh = true;
        } else {
            append.show();
        }
        if (refresh) {
            if (t.attr('addParams')) {
                console.log(collectParams($(t.attr('addParams'))));
                load(append, $.post(t.attr('append'), collectParams($(t.attr('addParams')))), true);
            } else {
                load(append, $.get(t.attr('append')), true);
            }
        }
        $('input[appendId=' + appendId + ']').prop('checked', true);
        $('input[appendId=' + appendId + ']').attr('append', 'D');
    } else {
        append.hide();
        $('input[appendId=' + appendId + ']').prop('checked', false);
    }
}

function collectParams(tag) {
    var params = {};
    $('[param]', tag).each(function() {
        var p = $(this);
        params[p.attr('param')] = p.text();
    })
    $('[predifined]', tag).each(function() {
        var p = $(this);
        params[p.attr('predifined')] = p.attr('value');
    })
    return params;
}

function makeEditable(tag, onEnter, onFocusOut, onNullify) {
    tag.on('keydown', function(e) {
        if(e.keyCode == 27) {
            tag.html(tag.agattr('def'));
        }
        if(e.keyCode == 13) {
            e.preventDefault();
            onEnter();
        }
    });
    tag.on('keyup', function(e) {
        if (tag.html() == tag.attr('def'))
            tag.removeClass("edited");
        else
            tag.addClass("edited");
    });
    tag.focusin(function() {
        tag.attr('def', tag.html());
    });
    tag.focusout(function() {
        if (!tag.is($(document.activeElement))) {
            console.log(document.activeElement);
            onFocusOut();
        }
    });
    if (tag.attr('editNullify')) {
        var nullify = $('<span class="nullify">')
        tag.after(nullify);
        nullify.on('click', function () {
            onNullify();
        })
    }
}

function applyModTag() {
    var t = $(this);
    if (t.prop("checked")) $(t.attr('modTag')).addClass(t.attr('modClass')); else $(t.attr('modTag')).removeClass(t.attr('modClass'));
}

function openTab(t) {
    t.addClass('active');
    $('.' + t.attr('tabContent')).each(showAndAutoload);
    var hash = "";
    $('.active[tabContent]').each(function () {
        hash += '#' + $(this).attr('tabContent');
    });
    window.location.hash = hash;
}

function loadGet(tag, uri, doInit, replace) {
    load(tag, $.get(uri), doInit, replace);
}

function loadPost(tag, uri, param, doInit, replace) {
    load(tag, uri, doInit, replace);
}

function load(tag, loader, doInit, replace) {
    var t = $(tag);
    loader.done(function(data) {
        if (replace) {
            var data = $(data);
            t.replaceWith(data);
            t = data;
        } else {
            t.html(data);
        }
        if (doInit) init(t);
    }).fail(function(jqXHR) {
        alert(jqXHR.responseText);
    });
    event.stopPropagation();
}

function showAndAutoload() {
    $(this).show();
    autoload(this);
}

function autoload(tag) {
    $(tag).find('[autoload]:visible').addBack('[autoload]:visible').each(function () {
        var t = $(this);
        var uri = t.attr('autoload');
        t.removeAttr('autoload');
        loadGet(t, uri, true);
    })
}

function containsHash(hash) {
    return window.location.hash.split("#").indexOf(hash) > 0;
}

function splitup() {
    $("[split]>div").height("auto");
    $("[split]>div").width("auto");
    $("[split]").each(function() {
        var t = $(this);
        var s = t.attr('split') === 'horizontal';
        var src = t.parent().attr('split') === undefined ? t.parent() : t;
        var w = src.width();
        var h = src.height();
        var children = t.children();
        children.each(function() {
            var c = $(this);
            if (s) {
                c.height(h);
                c.width((w - 50) / children.length);
            } else {
                c.width(w);
                c.height(h / children.length);
            }
        })
    })
    $("[autoscale]").each(function() {
        var t = $(this);
        var scale = Math.min(t.parent().height() / t.height(), t.parent().width() / t.width());
        t.css({transform: 'scale('+scale+","+scale+')'});
    });
}

$(function() {
    init(document);
    $(window).resize(splitup);
});