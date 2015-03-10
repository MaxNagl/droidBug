function init(tag) {
    $('[tabContent]', tag).each(function(){
		var t = $(this);
        t.unbind('click');
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
	$('[editurl]', tag).each(function(){
		var t = $(this);
		t.attr('contentEditable', 'true');
        t.unbind('keydown');
		t.on('keydown', function(e) {
			if(e.keyCode == 13) {
				e.preventDefault();
				$.post(t.attr('editurl'), {o : t.text()}).done(function(data) {
					t.html(data);
				}).fail(function(jqXHR) {
					alert(jqXHR.responseText);
				});
			}
		});
        if (t.attr('editNullify')) {
            var nullify = $('<span class="nullify">')
            t.after(nullify);
            nullify.on('click', function () {
                load(t, t.attr('editurl'), false);
            })
        }
	});
    $('[expand]', tag).each(function(){
        var t = $(this);
        var handle = $('<span class="closed">');
        t.prepend(handle);
        handle.on('click', function() {
            toggleExpand(t, handle);
        })
        if (t.attr('expand').substr(0, 1) == '!') {
            t.attr('expand', t.attr('expand').substring(1));
            toggleExpand(t, handle);
        }
    });
    $('[invoke]', tag).each(function(){
        var t = $(this);
        var invoker = $('<span class="invoke">');
        t.append(invoker);
        invoker.on('click', function() {
            var expand = t.data('expand');
            if (expand === undefined) {
                t.data('expand', expand = $('<div class="expand">'));
                t.append(expand);
                var handle = $('<span class="opened">');
                t.prepend(handle);
                handle.on('click', function() {
                    toggleExpand(t, handle);
                })
                t.removeClass("notOpenable");
            }
            load(expand, t.attr('invoke'), true);
        })
    });
    //$('[invoke]', tag).each(function(){
    //    var t = $(this);
    //    var parent = t.parent();
    //    t.on('click', function() {
    //        $.post(t.attr('invoke'), {o : t.text()}).done(function(data) {
    //            var handle = $('<span class="opened">');
    //            parent.prepend(handle);
    //            parent.removeClass("notOpenable");
    //            console.log(data);
    //            handle.on('click', function() {
    //                toggleExpand(t, handle);
    //            })
    //        }).fail(function(jqXHR) {
    //            alert(jqXHR.responseText);
    //        });
    //    })
    //});
    autoload(tag);
}

function toggleExpand(t, handle) {
    var expand = t.data('expand');
    if (expand === undefined) {
        t.data('expand', expand = $('<div class="expand">'));
        t.append(expand);
        if (t.attr('expand') != undefined)
            load(expand, t.attr('expand'), true);
    } else {
        expand.toggle();
    }
    handle.removeClass("closed").removeClass("opened").addClass(expand.is(':visible') ? "opened" : "closed");
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

function load(tag, uri, doInit) {
    $.get(uri).done(function(data) {
        $(tag).html(data);
        if (doInit) init($(tag));
    }).fail(function(jqXHR) {
        alert(jqXHR.responseText);
    });
}

function showAndAutoload() {
    $(this).show();
    autoload(this);
}

function autoload(tag) {
    $(tag).find('[autoload]:visible').addBack('[autoload]:visible').each(function () {
        var t = $(this);
        var uri = t.attr('autoload');
        $(tag).removeAttr('autoload');
        load(t, uri, true);
    })
}

function containsHash(hash) {
    return window.location.hash.split("#").indexOf(hash) > 0;
}

$(function() {
	init(document);
});