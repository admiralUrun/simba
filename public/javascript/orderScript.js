jQuery.exists = function (selector) {
    return ($(selector).length > 0);
}
jQuery.isDefended = function (object) {
    return object !== null
}

function addToOrder(item, title, cost) {
    function addElementForDisplay(isEmpty, text, cost) {
        function addElementForDisplay(index) {
            $(`<li class="list-group-item" id="element${index}">${text} <button type="button" class="close" aria-label="Close" onclick="deleteFromOrder(${index}, ${cost})"><span aria-hidden="true">&times;</span></button></li>`).appendTo('ul')
        }

        if (isEmpty) {
            addElementForDisplay(0)
        } else {
            addElementForDisplay(order.val().split(',').length - 1)
        }
    }

    let order = $('#inOrder')
    let inOrder = order.val()
    let isValueEmpty = inOrder === ''
    if (isValueEmpty) {
        order.val(item)
        changeTotal(Number(cost))
    } else {
        order.val(inOrder + ', ' + item)
        changeTotal(Number(cost))
    }
    console.log(order.val())
    addElementForDisplay(isValueEmpty, title, cost)
}

function changeTotal(cost) {
    let total = $('#total')
    let totalText = $('#totalText')
    total.val(Number(total.val()) + cost)
    totalText.html(total.val() + ' ₴')
}

function deleteFromOrder(indexToRemove, cost) {
    let total = $('#total')
    let totalText = $('#totalText')

    function getNewValueForInput(array) {
        let result = ''
        for (let i = 0; i < array.length; i++) {
            if (i === indexToRemove) {
            } else if (result === '') {
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
    total.val(Number(total.val()) - Number(cost))
    totalText.html(total.val() + '₴')
}

function changeButton(trueLabel, falseLabel, inputID, cost) {
    function getNewClassForButton() {
        let classArray = button.attr('class').split(" ")
        classArray.pop()
        if (isChecked) {
            return classArray.join(" ") + ' btn-danger'
        } else {
            return classArray.join(" ") + ' btn-success'
        }
    }

    let input = $(`#${inputID}`)
    let button = $(`#${inputID}-button`)
    let isChecked = (input.val() === 'true')
    if (isChecked) {
        button.text(falseLabel)
        changeTotal(Number(-cost))
    } else {
        button.text(trueLabel)
        changeTotal(Number(cost))
    }
    button.attr('class', getNewClassForButton())
    input.val(!isChecked)
}

function takeJSONFromDateSetCustomerForOrder(customerID) {
    function setCustomerForOrder(customer) {
        function getValeOrEmptyString(s) {
            if ($.isDefended(s)) return ' ' + s;
            else return ''
        }

        function addCustomerToOrderAndToUI(c) {
            function getAFullAddress() {
                return c.address + getValeOrEmptyString(c.entrance) + getValeOrEmptyString(c.floor) + getValeOrEmptyString(c.flat)
            }

            $(`#customersID`).val(`${c.id}`)
            $(`<h4 id="customer">${c.firstName} ${getValeOrEmptyString(c.lastName)} Телефон:${c.phone}  Місто:${c.city} Адреса:${getAFullAddress()}</h4>
                    <button id="editButton" class="btn btn-success" onclick="editCustomerInOrder()">Знайти іншого</button>`) // FOR first time
                .appendTo($('#customerInformation'))
        }

        function allSearchElementsStyleDisplayNone() {
            $("#searchDIV").hide()
        }

        allSearchElementsStyleDisplayNone()
        addCustomerToOrderAndToUI(customer)
    }

    let json = $(`#${customerID}`).data('customerJSON')
    setCustomerForOrder(json)
}

function takeCustomerJSONAddToUI(JSONArray) {
    function addDropItem(Customer) {
        let elementExists = document.getElementById(`customer${Customer.id}`);
        if (elementExists) {
        } else {
            $(`<a id="customer${Customer.id}" class="dropdown-item start" href="#" onclick="takeJSONFromDateSetCustomerForOrder(\`customer${Customer.id}\`)">${Customer.firstName} ${Customer.lastName} ${Customer.address}</a>`)
                .appendTo($("#searchMenu")
                )
            $(`#customer${Customer.id}`).data('customerJSON', Customer)
        }
    }

    JSONArray.forEach(function (c) {
        addDropItem(c)
    })
}

function editCustomerInOrder() {
    $(`#customersID`).val("")
    $(`#customer`).remove()
    $(`#editButton`).remove()
    $("#searchDIV").show()
}

function cleanDropMenu() {
    document.getElementById('searchMenu').innerHTML = ''
    $(`<span class="dropdown-header">Тут можна знайти Клієнта</span>`).appendTo($("#searchMenu"))
}

function setPayment(payment) {
    $(`<h5 id="paymentView">Спосіб оплати: ${payment}</h5>
        <button id="paymentEdit" class="btn btn-success"  onclick="removePayment()">Змінити</button>`).appendTo($('#payment'))
    $('#paymentMenu').hide()
    $('#paymentInput').val(payment).click()
}

function removePayment() {
    $('#paymentView').remove()
    $('#paymentEdit').remove()
    $('#paymentMenu').show()
    $('#paymentInput').val('')
}