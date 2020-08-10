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
jQuery.hideSearchDiv = function () {
    $("#searchDIV").hide()
}
jQuery.insertData = function (selector, key, value) {
    $(selector).data(key, value)
}

let customerInfoDiv = $("#customerInformation")
let total = $('#total')
let totalText = $('#totalText')

function addToOrder(item, title, cost) {
    function addElementForDisplay(text, value) {
        function addElementForDisplay(index, text, value) {
            $(`<li class="list-group-item" id="element${index}"> 
                ${text}
                <input id="inOrder[]" name="inOrder[]" value="${value}" style="display: none">
                <button type="button" class="close" aria-label="Close"
                onclick="deleteFromOrder('${index}', '${cost}')">
                <span aria-hidden="true">&times;</span></button>
            </li>`).appendTo('ul')
        }

        let count = $('#orderList').children('li').length
        addElementForDisplay(count, text, value)
    }

    changeTotal(Number(cost))
    addElementForDisplay(title, item)
}

function changeTotal(cost) {
    total.val(Number(total.val()) + cost)
    totalText.html(total.val() + ' ₴')
}

function deleteFromOrder(indexToRemove, cost) {
    $(`#element${indexToRemove}`).remove()
    total.val(Number(total.val()) - Number(cost))
    totalText.html(total.val() + ' ₴')
}

/**
 * Used inside createOrEditOrderTemplate
 * */
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
        function addDropItem(customerAddresses) {
            if (customerAddresses.addresses.length > 1) {
                $(`<a id="customer${customerAddresses.customer.id}" class="dropdown-item"
                    onclick="setCustomerAddAddressDropdown('${customerAddresses.customer.id}')" href="#">
                        ${customerAddresses.customer.firstName} ${customerAddresses.customer.phone} ${$.getValeOrEmptyString(customerAddresses.customer.instagram)}
                    </a>`
                ).appendTo($("#searchMenu"))
            } else if (customerAddresses.addresses.length !== 0) {
                $(`<a id="customer${customerAddresses.customer.id}" class="dropdown-item" 
                    onclick="setCustomerAndAddress('${customerAddresses.customer.id}')" href="#">
                        ${customerAddresses.customer.firstName} ${customerAddresses.customer.phone} ${$.getValeOrEmptyString(customerAddresses.customer.instagram)}
                    </a>`
                ).appendTo($("#searchMenu"))
            } else {}

            $(`#customer${customerAddresses.customer.id}`).data('customerJSON', customerAddresses)
        }
        if (!$.exists(`#customer${customerAddresses.customer.id}`)) {
            addDropItem(customerAddresses)
        }
    }

    JSONArray.forEach(function (c) {
        addDropItem(new CustomerAddresses(c))
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
    $(`<div id="customer" class="col-auto">
            <dl class="col-auto">
                ${descriptionListElement('Ім\'я', customer.firstName + ' ' + $.getValeOrEmptyString(customer.lastName))}
                ${descriptionListElement('Телефони', getPhones(customer))}
                ${descriptionListElement('Instagram', $.getValeOrEmptyString(customer.instagram))}
                ${descriptionListElement('Нотатка', $.getValeOrEmptyString(customer.notes))}
            </dl>
           <div class="col">
                <button id="editCustomerButton" class="btn btn-success" onclick="editCustomerInOrder()">Знайти іншого</button>
           </div>
           </div>`).appendTo(customerInfoDiv)
}

function displayAddress(address) {
    $(`<div id="address" class="col-auto">
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
          </div>`).appendTo(customerInfoDiv)
}

function setCustomerAddAddressDropdown(id, customerAddresses) {
    function setCustomerCreateDropDownFormAddresses(customerAddresses) {
        $.hideSearchDiv()
        setCustomer(customerAddresses.customer)
        createDropDownForAddresses(customerAddresses.customer.id, customerAddresses.addresses)
    }

    if(customerAddresses)  setCustomerCreateDropDownFormAddresses(customerAddresses)
    else setCustomerCreateDropDownFormAddresses($(`#customer${id}`).data(`customerJSON`))
}

function setAddress(address, id) {
    function setAddress(address) {
        $(`#address`).remove()
        $(`#addressId`).val(Number(address.id))
        displayAddress(address)
    }

    if($.isDefended(address)) setAddress(address)
    else setAddress($(`#address${id}`).data(`address`))
}

function setCustomer(customer) {
    $(`#customerId`).val(Number(customer.id))
    displayCustomer(customer)
}

function setCustomerAndAddress(customerID, customerAddress) {
    function setCustomerForOrder(customerAddress) {
        function addCustomerAddressToOrderAndToUI(customer, address) {
            setCustomer(customer)
            setAddress(address, address.id)
        }

        addCustomerAddressToOrderAndToUI(customerAddress.customer, customerAddress.addresses[0])
        $.hideSearchDiv()
    }

    if(customerAddress) setCustomerForOrder(customerAddress)
    else setCustomerForOrder($(`#customer${customerID}`).data('customerJSON'))
}

function descriptionListElement(head, content, needDivWithCol) {
    if($.isEmpty(head) || $.isEmpty(content) || head === '' || content === '') return ''
    else {
        if (needDivWithCol) return `<div class="col">
                    <dt>${head}</dt>
                    <dd>${content}</dd>
                </div>`
        else return`
                <dt>${head}</dt>
                <dd>${content}</dd>`
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

function createDropDownForAddresses(id, addresses) {
    $(`<div id="addresses" class="col-auto btn-group dropright">
            <div>
                <button type="button" class="btn btn-info start dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    Адреси 
                </button>
            <div id="customer${id}Addresses" class="dropdown-menu"></div>
            </div>
            </div>`).appendTo($('#customerInformation'))
    addresses.forEach( function (address) {
        $(`<a id="address${address.id}" class="dropdown-item" onclick="setAddress(null, '${address.id}')">
                            ${address.city} ${$.getValeOrEmptyString(address.residentialComplex)}
                            ${address.address}
                            ${$.getValeOrEmptyString(address.flat)}
                        </a>`).appendTo($(`#customer${id}Addresses`))
        $(`#address${address.id}`).data(`address`, address)
    })
}

function setCustomerAndAddressAddDropdown(customerAddresses) {
    let addressId = Number($(`#addressId`).val())
    setCustomer(customerAddresses.customer)
    if(customerAddresses.addresses.length > 1) {
        let address = customerAddresses.addresses.filter(function (a) {
            return a.id === addressId
        })
        createDropDownForAddresses(customerAddresses.customer.id, customerAddresses.addresses)
        setAddress(address[0], addressId)
    } else {
        setAddress(customerAddresses.addresses[0], addressId)
    }

}

/**
 * Used inside createOrEditOrderTemplate
 * */
function setPayment(payment) {
    $(`<h5 id="paymentView">Спосіб оплати: ${payment}</h5>
        <button id="paymentEdit" class="btn btn-success"  onclick="removePayment()">Змінити</button>`).appendTo($('#paymentDIV'))
    $('#paymentMenu').hide()
    $('#payment').val(payment)
}

function removePayment() {
    $('#paymentView').remove()
    $('#paymentEdit').remove()

    $('#paymentMenu').show()
    $('#payment').val('')
}


class Customer {
    constructor(customer) {
        this.id = customer.id
        this.firstName = customer.firstName
        this.lastName = customer.lastName
        this.phone = customer.phone
        this.phoneNote = customer.phoneNote
        this.phone2 = customer.phone2
        this.phoneNote2 = customer.phoneNote2
        this.instagram = customer.instagram
        this.preferences = customer.preferences
        this.notes = customer.notes
    }
}

class Address {
    constructor(address) {
        this.id = address.id
        this.customerId = address.customerId
        this.city = address.city
        this.residentialComplex = address.residentialComplex
        this.address = address.address
        this.entrance = address.entrance
        this.floor = address.floor
        this.flat = address.flat
        this.notesForCourier = address.notesForCourier
    }
}

class CustomerAddresses {
    constructor(object) {
        this.customer = new Customer(object.customer)
        this.addresses = object.addresses.map(a => new Address(a))
    }
}