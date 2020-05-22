function addToOrder(item) {
    console.log(item);
    let order = $('#inOrder')
    let inOrder = order.val()
    let isValueEmpty = inOrder === ''
    if (isValueEmpty) {
        order.val(item)
        addElementForDisplay(isValueEmpty, item)
    } else {
        order.val(inOrder + ', ' + item)
        addElementForDisplay(isValueEmpty, item)
    }
}

function addElementForDisplay(isEmpty, text) {
    function addElementForDisplay(index) {
        $('<li>'+ text + '<button type="button" class="close" aria-label="Close">\n' +
            '  <span aria-hidden="true">&times;</span>\n' +
            '</button></li>\n', {
            id: 'element' + index,
            type: 'button',
            class: 'list-group-item',
            "onclick": deleteFromOrder(index)
        }).appendTo('ul')
    }
    let order = $('#inOrder')
    if(isEmpty) {
        addElementForDisplay(0)
    } else {
        let inOrder = order.val().split(',')
        addElementForDisplay(inOrder.length - 1)
        console.log(inOrder.length)
    }
}

function deleteFromOrder(index) {
console.log('Delete' + index)
}