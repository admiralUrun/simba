function addToOrder(item) {
    console.log(item);
    let order = $('#inOrder')
    let inOrder = order.val()
    let isValueEmpty = inOrder === ''
    if (isValueEmpty) {
        order.val(item)
        addElementForDisplay(isValueEmpty)
    } else {
        order.val(inOrder + ', ' + item)
        addElementForDisplay(isValueEmpty)
    }
}

function addElementForDisplay(isEmpty) {
    let order = $('#inOrder')
    let inOrder = order.val().split(',')
    if(isEmpty) {

    } else {

    }

    console.log(inOrder.length)
}

function deleteFromOrder(index) {

}