
function getError(result) {
    if (result.responseText != undefined) {
        return result.responseText;
    } else {
        return 'HTTP Error: ' + result.status + " " + result.statusText;
    }
}

$(function () {
    loadModel(null, 'tabs.json', function(model) {
        $("#loading").replaceWith(model.view);
    });
});