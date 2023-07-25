import { baseUrl, createReq, forgotReq, loginReq, tokenReq } from "/api.js";

function getEmail() {
    document.cookie
}

$(document).ready(function() {

    console.log("Setting token behavior on token button.");
    $("#sendToken").click( function(){ tokenReq(); } );

    console.log("Setting RETURN keypress behavior.");
    $("#token").keypress(function(event) {
        if (event.keyCode === 13) { /* pressing RETURN */
            $("#sendToken").click();
        }
    });
});