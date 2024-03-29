jQuery.exists = function (selector) {
    return ($(selector).length > 0);
}
jQuery.isDefended = function (object) {
    return object != null
}
jQuery.isEmpty = function (object) {
    return object == null
}
jQuery.getValeOrEmptyString = function (s) {
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
            } else {
            }

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
            if ($.isEmpty(phone) || $.isEmpty(note)) return ''
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
                <button id="editCustomerButton" class="btn btn-success" onclick="editCustomerInOrder('${customer.id}') ">Знайти іншого</button>
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

function setCustomerAddAddressDropdown(id) {
    function setCustomerCreateDropDownFormAddresses(customerAddresses) {
        $.hideSearchDiv()
        setCustomer(customerAddresses.customer)
        createDropDownForAddresses(customerAddresses.customer.id, customerAddresses.addresses)
    }
    const data = $(`#customer${id}`).data(`customerJSON`)
    const discount = $("#discount")

    if(discount.val() === "") {
        discount.val("")
    } else {
        discount.val(Number(discount.val()) - data.discount)
    }

    setCustomerCreateDropDownFormAddresses(data)
}

function setAddress(address, id) {
    function setAddress(address) {
        $(`#address`).remove()
        $(`#addressId`).val(Number(address.id)).trigger("change")
        displayAddress(address)
    }

    if ($.isDefended(address)) setAddress(address)
    else setAddress($(`#address${id}`).data(`address`))
}

function setCustomer(customer) {
    $(`#customerId`).val(Number(customer.id)).trigger("change")
    displayCustomer(customer)
}

function setCustomerAndAddress(customerId) {
    function setCustomerForOrder(customerAddress) {
        function addCustomerAddressToOrderAndToUI(customer, address) {
            setCustomer(customer)
            setAddress(address, address.id)
        }

        addCustomerAddressToOrderAndToUI(customerAddress.customer, customerAddress.addresses[0])
        $.hideSearchDiv()
    }
    const data = $(`#customer${customerId}`).data(`customerJSON`)
    const discount = $("#discount")
    console.log(data.discount)
    if(discount.val() === "") {
        discount.val("")
    } else {
        discount.val(Number(discount.val()) - data.discount)
    }
    setCustomerForOrder(data)
}

function descriptionListElement(head, content, needDivWithCol) {
    if ($.isEmpty(head) || $.isEmpty(content) || head === '' || content === '') return ''
    else {
        if (needDivWithCol) return `<div class="col">
                    <dt>${head}</dt>
                    <dd>${content}</dd>
                </div>`
        else return `
                <dt>${head}</dt>
                <dd>${content}</dd>`
    }
}

function editCustomerInOrder(id) {
    $(`#customersID`).val("").trigger("change")
    $(`#addressId`).val("").trigger("change")
    $(`#inviterId`).val("").trigger("change")

    $(`#customer`).remove()
    $('#address').remove()
    $('#addresses').remove()
    $('#inviterDiv').remove()
    $(`#editCustomerButton`).remove()

    $("#searchDIV").show()

    const data = $(`#customer${id}`).data(`customerJSON`)
    const discount = $("#discount")
    if(discount.val() === "") {
        discount.val("")
    } else {
        discount.val(Number(discount.val()) - data.discount)
    }
}

function cleanDropMenu(id, label) {
    document.getElementById(id).innerHTML = ''
    $(`<span class="dropdown-header">${label}</span>`).appendTo($(`#${id}`))
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
    addresses.forEach(function (address) {
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
    if (customerAddresses.addresses.length > 1) {
        let address = customerAddresses.addresses.filter(function (a) {
            return a.id === addressId
        })
        createDropDownForAddresses(customerAddresses.customer.id, customerAddresses.addresses)
        setAddress(address[0], addressId)
    } else {
        setAddress(customerAddresses.addresses[0], addressId)
    }
    $(`#searchDIV`).hide()
}

function addInviterSearch() {
    $(`<div id="inviterDiv">
            <form id="inviterForm" class="form-inline">
                <input id="inviterSearch" type="search" class="form-control mr-sm-4 dropdown-toggle" placeholder="Хто запросив?" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"
                onkeyup="clearInviterSearch()"
                >
                <div id="inviterMenu" class="dropdown-menu">
                    <span class="dropdown-header">Тут можна знайти того Хто запросив нового Клієнта</span>
                </div>
                <input type="button" id="inviterSubmit" value="Пошук" class="btn btn-primary btn-rounded btn-sm my-0" onclick="takeInviterAddToUI()">
            </form>
    </div>`).appendTo(customerInfoDiv)
}

function takeInviterAddToUI() {
    const searchParams = $("#inviterSearch").val()
    $.get("/searchCustomerInviterForOrder?search=" + searchParams, function (JSONArray) {
        function addDropItem(customer) {
            function addDropItem(customer) {
                $(`<a id="inviterCustomer${customer.id}" class="dropdown-item"
                    onclick="setInviter('${customer.id}')" href="#">
                        ${customer.firstName} ${customer.phone} ${$.getValeOrEmptyString(customer.instagram)}
                    </a>`).appendTo($("#inviterMenu"))
                $(`#inviterCustomer${customer.id}`).data('customerJSON', customer)
            }

            if (!$.exists(`#inviterCustomer${customer.id}`)) {
                addDropItem(customer)
            }
        }
        JSONArray.forEach(function (c) {
            addDropItem(new Customer(c))
        })
    })
}

function setInviter(id) {
    function setInviter(data) {
        $("#inviterId").val(data.id)
        $("#inviterForm").hide()
        $(`
            <div id="inviter">
                <b> Запросив: ${data.firstName} ${$.getValeOrEmptyString(data.lastName)}</b>
                <button class="btn btn-info" onclick="changeInviter()">Змінити</button>
            </div>
        `).appendTo($("#inviterDiv"))
    }
    setInviter($(`#inviterCustomer${id}`).data('customerJSON'))
    const discount = $(`#discount`)
    if($.isEmpty(discount.val())) {
        discount.val(50)
    } else {
        discount.val(Number(discount.val()) + 50)
    }
}

function changeInviter() {
    const discount = $(`#discount`)
    $("#inviterForm").show()
    if (discount.val() !== "") {
        discount.val(Number(discount.val()) - 50)
    }
    $("#inviterId").val("")
    $("#inviter").remove()
}

function clearInviterSearch() {
    let searchParams = $("#inviterSearch").val()
    if (searchParams.length === 0) cleanDropMenu('inviterMenu', 'Тут можна знайти того Хто запросив нового Клієнта');
    cleanDropMenu('inviterMenu', 'Тут можна знайти того Хто запросив нового Клієнта');
}

function setPayment(payment) {
    $(`<h5 id="paymentView">Спосіб оплати: ${payment}</h5>
        <button id="paymentEdit" class="btn btn-success"  onclick="removePayment()">Змінити</button>`).appendTo($('#paymentDIV'))
    $('#paymentMenu').hide()
    $('#payment').val(payment).trigger("change")
}

function removePayment() {
    $('#paymentView').remove()
    $('#paymentEdit').remove()

    $('#paymentMenu').show()
    $('#payment').val('').trigger("change")
}

function listener (input) {
    if(input.value !== "") {
        input.classList.remove('is-invalid')
        input.classList.add('is-valid')
    } else {
        input.classList.remove('is-valid')
        input.classList.add('is-invalid')
    }
    const is_valid = $('.form-control.control').length === $('.form-control.control.is-valid').length;
    $("#submitBtn").attr("disabled", !is_valid)
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
        this.discount = object.discount
    }
}