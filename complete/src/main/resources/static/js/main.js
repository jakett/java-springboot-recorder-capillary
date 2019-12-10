function setActivatedUser(type, value) {
    console.log("TVT go to setActivatedUser function, type = " + type);
    console.log("TVT go to setActivatedUser function, value = " + value);

    var dataJson = {
        type: type,
        value: value
    };

    $.ajax({
        type: "POST",
        contentType: "application/json; charset=utf-8",
        traditional: true,
        url: "/userlist",
        data: JSON.stringify(dataJson),
        dataType: 'json',
        cache: false,
        timeout: 600000,
        success: function (data) {
            console.log("TVT success, data = " + data);
        },
        error: function(e) {

        }
    })
}