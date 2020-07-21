jQuery.exists = function (selector) {
    return ($(selector).length > 0);
}

function addToDisplay(id) {
    function addToDisplay(recipe) {
        let inputs = document.querySelectorAll("#formIDHere input[name='recipesId[]']").length;
        console.log(inputs)
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
    if(!$.exists(`recipe${recipe.id}`)) {
        let item = $(`<a id="recipe${recipe.id}" class="dropdown-item" onclick="addToDisplay('recipe${recipe.id}')">${recipe.name}</a>`)
        item.data("data", recipe)
        item.appendTo($("#searchMenu"))
    }
}

function cleanDropMenu() {
    document.getElementById('searchMenu').innerHTML = ''
    $(`<span class="dropdown-header">Тут можна знайти Рейепти</span>`).appendTo($("#searchMenu"))
}

function takeRecipesJSONAddToDropDown(jsonArray) {
    console.log(jsonArray)
    jsonArray.forEach(function (recipe) {
            addToDropDown(recipe)
        }
    )
    $("#searchMenu").focus()
}
