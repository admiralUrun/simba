jQuery.getValueOrDashIfStringIsEmpty = function (str) {
    if (str === "") return `-`; else return str;
}

let city = $(`#city`)
let residentialComplex = $(`#residentialComplex`)
let address = $(`#address`)
let entrance = $(`#entrance`)
let floor = $(`#floor`)
let flat = $(`#flat`)
let notesForCourier = $(`#notesForCourier`)
let tableBody = $("table tbody")

function encoderString(i) {
    function getEncodeValueOrEmptyString(selector, nameForEncoding) {
        if (selector.val() === '') return ""
        else return selector.val() + nameForEncoding
    }
    function checkForIds() {
        if(i != null && getInputArrayByIndex(i).val().indexOf("id") >= 0) {
            console.log(getInputArrayByIndex(i))
            let a = getInputArrayByIndex(i).val().split(',')
            return `${a.shift()},${a.shift()},`
        } else {
            return ""
        }
    }
    return Array(checkForIds(),
        getEncodeValueOrEmptyString(city, `(city)`),
        getEncodeValueOrEmptyString(residentialComplex, `(residentialComplex)`),
        getEncodeValueOrEmptyString(address, `(address)`),
        getEncodeValueOrEmptyString(entrance, `(entrance)`),
        getEncodeValueOrEmptyString(floor, `(floor)`),
        getEncodeValueOrEmptyString(flat, `(flat)`),
        getEncodeValueOrEmptyString(notesForCourier, `(notes)`)).filter(function (v) { return v !== '' }).join(',')
}

function cleanAllAddressInputs() {
    city.val("")
    residentialComplex.val("")
    address.val("")
    entrance.val("")
    floor.val("")
    flat.val("")
    notesForCourier.val("")
}

function getInputArrayByIndex(i) {
    return $(`input[id="addresses[${i}]"]`)
}

function addToAddressAndCleanInputs() {
    let addresses = $(`#addresses`)
    let isAddressesEmpty = addresses.val() === ''
    function addRowToDisplay(isEmpty) {
        function addToDisplay(rowIndex) {
            function getTb(tbID, information) {
                return `<td id="${tbID}">${$.getValueOrDashIfStringIsEmpty(information)}</td>`
            }
            function getInput(i, value) {
                return `<input id="addresses[${i}]" name="addresses[${i}]" value="${value}" style="display: none">`
            }
            let changeButton = `<button class="btn btn-info сol-auto" type="button" onclick="prepareToEdit('${rowIndex}')">Змінити</button>`
            let deleteButton = `<button class="btn btn-danger сol-auto" type="button" onclick="deleteRow('${rowIndex}')">Видалити</button>`
            let row = $(`<tr id='row-${rowIndex}'>
                ${getTb(`city-${rowIndex}`, city.val())}
                ${getTb(`residentialComplex-${rowIndex}`, residentialComplex.val())}
                ${getTb(`address-${rowIndex}`, address.val())}
                ${getTb(`entrance-${rowIndex}`, entrance.val())}
                ${getTb(`floor-${rowIndex}`, floor.val())}
                ${getTb(`flat-${rowIndex}`, flat.val())}
                ${getTb(`notes-${rowIndex}`, notesForCourier.val())}
                ${getTb(`settings`, changeButton + deleteButton + getInput(rowIndex, encoderString(null)))}
                </tr>`)
            tableBody.append(row)
        }

        if (isEmpty) addToDisplay(0)
        else {
            let count = $('#addressTableBody').children('tr').length
            addToDisplay(count)
        }
    }
    addRowToDisplay(isAddressesEmpty)
    cleanAllAddressInputs()
}

function deleteRow(indexToRemove) {
    let input = getInputArrayByIndex(indexToRemove)
    if (input.val().includes("id")) {
        let id = input.val().split(`,`).shift().replace("(id)", "")
        $(`<input id="addressesToDelete[${indexToRemove}]" name="addressesToDelete[${indexToRemove}]" value="${id}" style="display: none">`).appendTo($(`#mainForm`))
    } else {}
    $(`#row-${indexToRemove}`).remove()
    cleanAllAddressInputs()
    changeButton(`Додати`, addToAddressAndCleanInputs)
}

function prepareToEdit(i) {
    function rowToInputs(i) {
        function getValueOrEmptyStringIfDash(text) {
            if(text === `-`) return ''
            else return text
        }
        city.val(getValueOrEmptyStringIfDash($(`#city-`+ i).text()))
        residentialComplex.val(getValueOrEmptyStringIfDash($(`#residentialComplex-`).text()))
        address.val(getValueOrEmptyStringIfDash($(`#address-` + i).text()))
        entrance.val(getValueOrEmptyStringIfDash($(`#entrance-` + i).text()))
        floor.val(getValueOrEmptyStringIfDash($(`#floor-` + i).text()))
        flat.val(getValueOrEmptyStringIfDash($(`#flat-` + i).text()))
        notesForCourier.val(getValueOrEmptyStringIfDash($(`#notes-` + i).text()))
    }
    rowToInputs(i)
    changeButton(`Змінити`, editRow, i)
}

function changeButton(text, action, i) {
    let addressButton = $(`#addressBtn`)
    addressButton.text(text)
    if (i != null) addressButton.attr("onclick", `editRow('${i}')`)
    else addressButton.attr("onclick", `addToAddressAndCleanInputs('${i}')`)
}

function editRow(i) {
    function changeRow(i) {
        $(`#city-${i}`).text($.getValueOrDashIfStringIsEmpty(city.val()))
        $(`#residentialComplex-${i}`).text($.getValueOrDashIfStringIsEmpty(residentialComplex.val()))
        $(`#address-${i}`).text($.getValueOrDashIfStringIsEmpty(address.val()))
        $(`#entrance-${i}`).text($.getValueOrDashIfStringIsEmpty(entrance.val()))
        $(`#floor-${i}`).text($.getValueOrDashIfStringIsEmpty(floor.val()))
        $(`#flat-${i}`).text($.getValueOrDashIfStringIsEmpty(flat.val()))
        $(`#notes-${i}`).text($.getValueOrDashIfStringIsEmpty(notesForCourier.val()))
    }

    console.log(encoderString(i))
    getInputArrayByIndex(i).val(encoderString(i))
    changeRow(i)
    cleanAllAddressInputs()
    changeButton(`Додати`, addToAddressAndCleanInputs)
}