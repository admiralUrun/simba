function addToOrder(item, title) {
    let order = $('#inOrder')
    let inOrder = order.val()
    let isValueEmpty = inOrder === ''
    if (isValueEmpty) {
        order.val(item)
        addElementForDisplay(isValueEmpty, title)
    } else {
        order.val(inOrder + ', ' + item)
        addElementForDisplay(isValueEmpty, item)
    }
}

function addElementForDisplay(isEmpty, text) {
    function addElementForDisplay(index) {
        $(`<li class="list-group-item" id="element${index}">${text} <button type="button" class="close" aria-label="Close" onclick="deleteFromOrder(${index})"><span aria-hidden="true">&times;</span></button></li>`).appendTo('ul')
    }
    if(isEmpty) {
        addElementForDisplay(0)
    } else {
        addElementForDisplay($('#inOrder').val().split(',').length - 1)
    }
}

function deleteFromOrder(indexToRemove) {
    function getNewValueForInput(array) {
        let result = ''
        for (let i = 0; i < array.length; i++) {
            if (i === indexToRemove) {}
            else if (result === '') {
                result += array[i];
            } else {
                result += ',' + array[i]
            }
        }
        return result;
    }
    let order = $('#inOrder')
    if (order.val().indexOf(',') > 1) {
        order.val(getNewValueForInput(order.val().split(',')))
        $(`#element${indexToRemove}`).remove()
    } else {
        $(`#element${indexToRemove}`).remove()
        order.val('')
    }
}