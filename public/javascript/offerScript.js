jQuery.exists = function (selector) {
    return ($(selector).length > 0);
}

function addToDisplay(id) {
    function addToDisplay(recipe) {
        $(`<li id="offerView${recipe.id}">
            ${recipe.name}
            <input name="recipesId[]" value="${recipe.id}"  style="display:none;">
            <button type="button" class="close" aria-label="Close" onclick="deleteFromOffer('${recipe.id}')">
                <span aria-hidden="true">&times;</span>
                </button>
        </li>`).appendTo('ul')
    }

    addToDisplay($(`#${id}`).data("data"))
}

function deleteFromOffer(indexToDelete) {
    $(`#offerView${indexToDelete}`).remove()
}

function addToDropDown(recipe) {
    var layOutClass = ""
    if(recipe.wasUsed === 1) layOutClass = "bg-danger"
    else if(recipe.wasUsed === 2) layOutClass = "bg-success"
    else layOutClass = " "
    if (!$.exists(`#recipe${recipe.id}`)) {
        let item = $(`<a id="recipe${recipe.id}" class="dropdown-item ${layOutClass}" onclick="addToDisplay('recipe${recipe.id}')">${recipe.name}</a>`)
        item.data("data", recipe)
        item.appendTo($("#searchMenu"))
    }
}

function cleanDropMenu() {
    document.getElementById('searchMenu').innerHTML = ''
    $(`<span class="dropdown-header">Тут можна знайти Рейепти</span>`).appendTo($("#searchMenu"))
}

function takeRecipesJSONAddToDropDown(jsonArray) {
    jsonArray.forEach(function (recipe) {
            addToDropDown(recipe)
        }
    )
    $("#searchMenu").focus()
}
