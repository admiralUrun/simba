jQuery.exists = function(selector) {return ($(selector).length > 0);}
jQuery.isDefended = function(object) {return object !== null}
function addToOrder(item, title) {
    let order = $('#inOrder')
    let inOrder = order.val()
    let isValueEmpty = inOrder === ''
    if (isValueEmpty) {
        order.val(item)
    } else {
        order.val(inOrder + ', ' + item)
    }
    addElementForDisplay(isValueEmpty, title)
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

function changeButton(id, trueLabel, falseLabel, inputID) {
    function getNewClassForButton() {
        let classArray = button.attr('class').split(" ")
        classArray.pop()
        if (isChecked) {
           return classArray.join(" ") + ' btn-danger'
        } else {
            return classArray.join(" ") + ' btn-success'
        }
    }
    let input = $(`#`+ inputID)
    let button = $(`#` + id)
    let isChecked = (input.val() === 'true')
    if(isChecked) {
        button.text(falseLabel)
    } else {
        button.text(trueLabel)
    }
    button.attr('class', getNewClassForButton())
    input.val(!isChecked)
}

function takeJSONFromDateSetCustomerForOrder(customerID) {
    function setCustomerForOrder(customer) {
        function formatSting(s) {
            if($.isDefended(s)) return ' ' + s;
                else return ''
            }
            function addCustomerToOrderAndToUI(c) {
                function getAFullAddress() {
                    return c.address + formatSting(c.entrance) + formatSting(c.foolr) + formatSting(c.flat)
                }
                $(`#customersID`).val(`${c.id}`)
                $(`<h4 id="customer">${c.firstName} ${formatSting(c.lastName)} Телефон:${c.phone}  Місто:${c.city} Адреса:${getAFullAddress()}</h4>
                    <button id="editButton" class="btn btn-success" onclick="editCustomerInOrder()">Знайти іншого</button>`) // FOR first time
                    .appendTo($('#customerInformation'))
            }
            function allSearchElementsStyleDisplayNone() {
                $("#searchDIV").css('display', 'none')
            }
            allSearchElementsStyleDisplayNone()
            addCustomerToOrderAndToUI(customer)
    }
    let json = $(`#${customerID}`).data('customerJSON')
    console.log(json)
    setCustomerForOrder(json)
    }

function takeCustomerJSONAddToUI(JSONArray) {
    // <div id="dropdown-menu" className="dropdown-menu">
    //     <span className="dropdown-item-text">Dropdown item text</span>
    //     <a className="dropdown-item" href="#">Action</a>
    //     <a className="dropdown-item" href="#">Another action</a>
    //     <a className="dropdown-item" href="#">Something else here</a>
    // </div>
    function addDropItem(Customer) {
        let elementExists = document.getElementById(`customer${Customer.id}`);
        if(elementExists) {}
        else {
            $(`<a id="customer${Customer.id}" class="dropdown-item start" href="#" onclick="takeJSONFromDateSetCustomerForOrder(\`customer${Customer.id}\`)">${Customer.firstName} ${Customer.lastName} ${Customer.address}</a>`)
                .appendTo($("#searchMenu")
            )
            $(`#customer${Customer.id}`).data('customerJSON', Customer)
        }
    }
    JSONArray.forEach(function (c) {
        addDropItem(c)
    })
    // console.log("Okay that is works")
    // console.log(JSONArray)
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