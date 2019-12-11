
function setActivatedUser(type, email, activated) {
    if(type != 'activated' && activated == 'false') {
        alert("Please activated for this user!");
        return;
    }

    var dataJson = {
        type: type,
        value: email
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
            if(type == "activated") {
                reloadPage();
            }

        },
        error: function(e) {
            if(type == "activated") {
                reloadPage();
            }
        }
    })
}

function reloadPage() {
    setTimeout(function() {
        location.reload(true);
    }, 1000);
}

//function handleFiles(event) {
//    console.log(event);
//    if (event.target.files && event.target.files.length) {
//        var file = event.target.files[0];
//        console.log(file);
//        var formData = new FormData();
//        formData.append("image_file", file);
//
//        $.ajax({
//          url: "/userlist",
//          type: 'POST',
//          data: formData,
//          async: false,
//          cache: false,
//          contentType: false,
//          processData: false,
//          success: function (status) {
//
//          },
//          error: function(e) {
//          }
//        });
//    }
//}