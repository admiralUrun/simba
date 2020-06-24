let city = $(`#city`)
let residentialComplex = $(`#residentialComplex`)
let address = $(`#address`)
let entrance = $(`#entrance`)
let floor = $(`#floor`)
let flat = $(`#flat`)
let notesForCourier = $(`#notesForCourier`)

function encoderString(i) {
    function getEncodeValueOrEmptyString(selector, nameForEncoding, withComaAtStart) {
        if (selector.val() === '') return ""
        else {
            if(withComaAtStart) return `, ` + selector.val() + nameForEncoding
            else return selector.val() + nameForEncoding
        }
    }
    function checkForIds() {
        if(i !== null) {
            let a = getInputArrayByIndex(i).val().split(',')
            return `${a.shift()},${a.shift()},`
        } else {
            return ""
        }
    }
    return `(` +
        checkForIds() +
        getEncodeValueOrEmptyString(city, `(city)`, false) +
        getEncodeValueOrEmptyString(residentialComplex, `(residentialComplex)`, true) +
        getEncodeValueOrEmptyString(address, `(address)`, true) +
        getEncodeValueOrEmptyString(entrance, `(entrance)`, true) +
        getEncodeValueOrEmptyString(floor, `(floor)`, true) +
        getEncodeValueOrEmptyString(flat, `(flat)`, true) +
        getEncodeValueOrEmptyString(notesForCourier, `(notes)`, true) +
        `)`
}
function cleanAllInputs() {
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
    function addRowToTable() {
        let addresses = $(`#addresses`)
        let isAddressesEmpty = addresses.val() === ''
        function addRowToDisplay(isEmpty) {
            function addToDisplay(rowIndex) {
                function getTb(tbID, information) {
                    return `<td id="${tbID}">`+ getValueOrDashIfEmpty(information) +`</td>`
                }
                function getInput(i, value) {
                    return `<input id="addresses[${i}]" name="addresses[${i}]" value="${value}" style="display: none">`
                }
                let changeButton = `<button class="btn btn-info сol-auto" type="button" onclick="prepareToEdit('${rowIndex}')">Змінити</button>`
                let deleteButton = `<button class="btn btn-danger сol-auto" type="button" onclick="deleteRow('${rowIndex}')">Видалити</button>`
                let row = $(`<tr id='row-${rowIndex}'>`+
                    getTb(`city-${rowIndex}`, city.val()) +
                    getTb(`residentialComplex-${rowIndex}`, residentialComplex.val()) +
                    getTb(`address-${rowIndex}`, address.val()) +
                    getTb(`entrance-${rowIndex}`, entrance.val()) +
                    getTb(`floor-${rowIndex}`, floor.val()) +
                    getTb(`flat-${rowIndex}`, flat.val()) +
                    getTb(`notesForCourier-${rowIndex}`, notesForCourier.val()) +
                    getTb(`settings`, changeButton + deleteButton + getInput(rowIndex, encoderString(null))) +
                    `</tr>`)
                let tableBody = $("table tbody")
                tableBody.append(row)
            }

            if (isEmpty) addToDisplay(0)
            else {
                let count = $('#addressTableBody').children('tr').length
                addToDisplay(count)
            }
        }
        addRowToDisplay(isAddressesEmpty)
    }

    addRowToTable()
    cleanAllInputs()
}

function getValueOrDashIfEmpty(object) {
    if (object === "") return `-`
    else return object
}

function deleteRow(indexToRemove) {
    $(`#row-${indexToRemove}`).remove()
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
    addressButton.attr("onclick", `editRow('${i}')`)
}

function editRow(i) {
    function changeRow(i) {
        $(`#city-${i}`).text(getValueOrDashIfEmpty(city.val()))
        $(`#residentialComplex-${i}`).text(getValueOrDashIfEmpty(residentialComplex.val()))
        $(`#address-${i}`).text(getValueOrDashIfEmpty(address.val()))
        $(`#entrance-${i}`).text(getValueOrDashIfEmpty(entrance.val()))
        $(`#floor-${i}`).text(getValueOrDashIfEmpty(floor.val()))
        $(`#flat-${i}`).text(getValueOrDashIfEmpty(flat.val()))
        $(`#notes-${i}`).text(getValueOrDashIfEmpty(notesForCourier.val()))
    }

    console.log(encoderString(i))
    getInputArrayByIndex(i).val(encoderString(i))
    changeRow(i)
    cleanAllInputs()
    changeButton(`Додати`, addToAddressAndCleanInputs)
}

/*
* TODO: Deal with id's
* TODO: Think how remove address on back-end ?
*
* */