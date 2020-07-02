jQuery.exists = function (selector) {
    return ($(selector).length > 0);
}
jQuery.isDefended = function (object) {
    return object != null
}
jQuery.isEmpty = function (object) {
    return object == null
}
jQuery.getValeOrEmptyString = function(s) {
    if ($.isDefended(s)) return ' ' + s;
    else return ''
}
jQuery.selectorToHidden = function (selector) {
    $(selector).hide()
}
jQuery.insertData = function (selector, key, value) {
    $(selector).data(key, value)
}

let customerInfoDiv = $("#customerInformation")
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

function takeCustomerJSONAddToUI(JSONArray) {
    function addDropItem(customerAddresses) {
        function addDropItem() {
            function gettingDropDownItems(array) {
                let dropDowns = array.map(function (address) {
                    return `<a class="dropdown-item" onclick="setAddress( '${address.id}')"
                        >${address.city} ${address.residentialComplex} ${address.address} ${address.entrance} ${address.floor} ${address.flat}</a>`
                })
                return dropDowns.join(`\n`)
            }
            if (customerAddresses.addresses.length > 1) {
                $(`<a id="customer${customerAddresses.customer.id}" class="dropdown-item" href="#"
                    onclick="setCustomerCreateDropDownFormAddresses('${customerAddresses.customer.id}')"
                    >${customerAddresses.customer.firstName} ${customerAddresses.customer.phone} ${$.getValeOrEmptyString(customerAddresses.customer.instagram)}
                    </a>`
                ).appendTo($("#searchMenu"))
            } else if (customerAddresses.addresses.length !== 0) {
                $(`<a id="customer${customerAddresses.customer.id}" class="dropdown-item" href="#"
                    onclick="setCustomerAndAddress('${customerAddresses.customer.id}', '${customerAddresses.addresses[0].id}')"
                    >${customerAddresses.customer.firstName} ${customerAddresses.customer.phone} ${$.getValeOrEmptyString(customerAddresses.customer.instagram)}
                    </a>`
                ).appendTo($("#searchMenu"))
            } else {}
            $(`#customer${customerAddresses.customer.id}`).data('customerJSON', customerAddresses)
        }

        if (!$.exists(`#customer${customerAddresses.customer.id}`)) {
            addDropItem()
        } else {}
    }

    JSONArray.forEach(function (c) {
        addDropItem(c)
    })
}

function displayCustomer(customer) {
    function getPhones(customer) {
        function secondPhone(phone, note) {
            if($.isEmpty(phone) || $.isEmpty(note)) return ''
            else return " " + `Другий Телефон:` + phone + $.getValeOrEmptyString(note)
        }
        return `Телефони:` + customer.phone + ` ` + $.getValeOrEmptyString(customer.phoneNote) + secondPhone(customer.phone2, customer.phoneNote2)
    }
    $(`
          <div id="customer" class="col-auto">
            <dl class="col-auto">
                ${descriptionListElement('Ім\'я', customer.firstName + ' ' + $.getValeOrEmptyString(customer.lastName))}
                ${descriptionListElement('Телефони', getPhones(customer))}
                ${descriptionListElement('Instagram', $.getValeOrEmptyString(customer.instagram))}
                ${descriptionListElement('Нотатка', $.getValeOrEmptyString(customer.notes))}
            </dl>
           <div class="col">
                <button id="editCustomerButton" class="btn btn-success" onclick="editCustomerInOrder()">Знайти іншого</button>
           </div>
           </div>
        `).appendTo(customerInfoDiv)
}

function displayAddress(address, thereIsOnlyOneAddress) {
    $(`        
          <div id="address" class="col-auto">
            <dl class="col-auto">
                <div class="row">
                    ${descriptionListElement("Місто", address.city, true)}
                    ${descriptionListElement("ЖК", address.residentialComplex, true)}
                </div>
            ${descriptionListElement("Адреса", address.address)}
                <div class="row">
                    ${descriptionListElement('Під\'їзд', address.entrance, true)}
                    ${descriptionListElement('Поверх', address.floor, true)}
                    ${descriptionListElement('Квартира', address.flat, true)}
                </div>
            ${descriptionListElement('Нотатка', address.notesForCourier)}
          </dl>
          </div>
        `).appendTo(customerInfoDiv)
}

function setCustomerCreateDropDownFormAddresses(id) {
    function setCustomerCreateDropDownFormAddresses(customerAddresses) {
        function createDropDownForAddresses(addresses) {
            $(`
           <div id="addresses" class="col-auto btn-group dropright">
            <div>
                <button type="button" class="btn btn-info start dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    Адреси 
                </button>
            <div id="customer${customerAddresses.id}Addresses" class="dropdown-menu"></div>
            </div>
            </div>`
            ).appendTo($('#customerInformation'))
            addresses.forEach( function (address) {
                $(`<a id="address${address.id}" class="dropdown-item" onclick="setAddress(null, false, '${address.id}')">
                            ${address.city} ${$.getValeOrEmptyString(address.residentialComplex)}
                            ${address.address}
                            ${$.getValeOrEmptyString(address.flat)}
                        </a>`).appendTo($(`#customer${customerAddresses.id}Addresses`))
                $(`#address${address.id}`).data(`address`, address)
                }
            )
        }

        $.selectorToHidden("#searchDIV")
        setCustomer(customerAddresses.customer)
        createDropDownForAddresses(customerAddresses.addresses)
    }

    setCustomerCreateDropDownFormAddresses($(`#customer${id}`).data(`customerJSON`))
}

function setAddress(address, thereIsOnlyOneAddress, id) {
    function setAddress(address, thereIsOnlyOneAddress) {
        $(`#address`).remove()
        $(`#addressID`).val(`${address.id}`)
        displayAddress(address, thereIsOnlyOneAddress)
    }

    if($.isDefended(address)) setAddress(address, thereIsOnlyOneAddress)
    else setAddress($(`#address${id}`).data(`address`), thereIsOnlyOneAddress)
}
function setCustomer(customer) {
    $(`#customersID`).val(`${customer.id}`)
    displayCustomer(customer)
}

function setCustomerAndAddress(customerID, addressID) {
    function setCustomerForOrder(customerAddress) {
        function addCustomerAddressToOrderAndToUI(customer, address) {
            setCustomer(customer)
            setAddress(address, true)
        }

        addCustomerAddressToOrderAndToUI(customerAddress.customer, customerAddress.addresses[0])
        $.selectorToHidden("#searchDIV")
    }

    let json = $(`#customer${customerID}`).data('customerJSON')
    setCustomerForOrder(json)
}

function descriptionListElement(head, content, needDivWithCol) {
    if($.isEmpty(head) || $.isEmpty(content) || head === '' || content === '') return ''
    else {
        if(needDivWithCol) {
            return `
                <div class="col">
                <dt>
                    ${head}
                </dt>
                <dd>
                    ${content}
                </dd>
                </div>
            `
        } else {
            return`
                <dt>
                    ${head}
                </dt>
                <dd>
                    ${content}
                </dd>
            `
        }
    }
}

function editCustomerInOrder() {
    $(`#customersID`).val("")
    $(`#customer`).remove()
    $('#address').remove()
    $('#addresses').remove()
    $(`#editCustomerButton`).remove()
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