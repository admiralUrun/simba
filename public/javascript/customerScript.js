jQuery.getValueOrDashIfStringIsEmpty = function (str) {
    if (str === "") return `-`; else return str;
}

let cityInput = $(`#city`)
let residentialComplexInput = $(`#residentialComplex`)
let addressInput = $(`#address`)
let entranceInput = $(`#entrance`)
let floorInput = $(`#floor`)
let flatInput = $(`#flat`)
let notesForCourierInput = $(`#notesForCourier`)
let tableBody = $("table tbody")

function encoderString(i) {
    function getEncodeValueOrEmptyString(selector, nameForEncoding) {
        if (selector.val() === '') return ""
        else return selector.val() + nameForEncoding
    }

    function checkForIds() {
        if (i != null && getAddressInputArrayByIndex(i).val().indexOf("id") >= 0) {
            console.log(getAddressInputArrayByIndex(i))
            let a = getAddressInputArrayByIndex(i).val().split(',')
            return `${a.shift()},${a.shift()},`
        } else return ""
    }

    return Array(checkForIds(),
        getEncodeValueOrEmptyString(cityInput, `(city)`),
        getEncodeValueOrEmptyString(residentialComplexInput, `(residentialComplex)`),
        getEncodeValueOrEmptyString(addressInput, `(address)`),
        getEncodeValueOrEmptyString(entranceInput, `(entrance)`),
        getEncodeValueOrEmptyString(floorInput, `(floor)`),
        getEncodeValueOrEmptyString(flatInput, `(flat)`),
        getEncodeValueOrEmptyString(notesForCourierInput, `(notes)`)).filter(function (v) {
        return v !== ''
    }).join(',')
}

function cleanAllAddressInputs() {
    cityInput.val("")
    residentialComplexInput.val("")
    addressInput.val("")
    entranceInput.val("")
    floorInput.val("")
    flatInput.val("")
    notesForCourierInput.val("")
}

function getAddressInputArrayByIndex(i) {
    return $(`input[id="addresses[${i}]"]`)
}

function addToAddressAndCleanInputs() {
    function addRowToDisplay() {
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
                ${getTb(`city-${rowIndex}`, cityInput.val())}
                ${getTb(`residentialComplex-${rowIndex}`, residentialComplexInput.val())}
                ${getTb(`address-${rowIndex}`, addressInput.val())}
                ${getTb(`entrance-${rowIndex}`, entranceInput.val())}
                ${getTb(`floor-${rowIndex}`, floorInput.val())}
                ${getTb(`flat-${rowIndex}`, flatInput.val())}
                ${getTb(`notes-${rowIndex}`, notesForCourierInput.val())}
                ${getTb(`settings`, changeButton + deleteButton + getInput(rowIndex, encoderString(null)))}
                </tr>`)
            tableBody.append(row)
        }

        let count = $('#addressTableBody').children('tr').length
        addToDisplay(count)
    }

    addRowToDisplay()
    cleanAllAddressInputs()
}

function deleteRow(indexToRemove) {
    $(`#row-${indexToRemove}`).remove()
    cleanAllAddressInputs() // TODO: under the question
    changeButton(`Додати`, addToAddressAndCleanInputs)
}

function prepareToEdit(i) {
    function rowToInputs(i) {
        function getValueOrEmptyStringIfDash(text) {
            if (text === `-`) return ''
            else return text
        }

        cityInput.val(getValueOrEmptyStringIfDash($(`#city-` + i).text()))
        residentialComplexInput.val(getValueOrEmptyStringIfDash($(`#residentialComplex-` + i).text()))
        addressInput.val(getValueOrEmptyStringIfDash($(`#address-` + i).text()))
        entranceInput.val(getValueOrEmptyStringIfDash($(`#entrance-` + i).text()))
        floorInput.val(getValueOrEmptyStringIfDash($(`#floor-` + i).text()))
        flatInput.val(getValueOrEmptyStringIfDash($(`#flat-` + i).text()))
        notesForCourierInput.val(getValueOrEmptyStringIfDash($(`#notes-` + i).text()))
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
        $(`#city-${i}`).text($.getValueOrDashIfStringIsEmpty(cityInput.val()))
        $(`#residentialComplex-${i}`).text($.getValueOrDashIfStringIsEmpty(residentialComplexInput.val()))
        $(`#address-${i}`).text($.getValueOrDashIfStringIsEmpty(addressInput.val()))
        $(`#entrance-${i}`).text($.getValueOrDashIfStringIsEmpty(entranceInput.val()))
        $(`#floor-${i}`).text($.getValueOrDashIfStringIsEmpty(floorInput.val()))
        $(`#flat-${i}`).text($.getValueOrDashIfStringIsEmpty(flatInput.val()))
        $(`#notes-${i}`).text($.getValueOrDashIfStringIsEmpty(notesForCourierInput.val()))
    }

    console.log(encoderString(i))
    getAddressInputArrayByIndex(i).val(encoderString(i))
    changeRow(i)
    cleanAllAddressInputs()
    changeButton(`Додати`, addToAddressAndCleanInputs)
}