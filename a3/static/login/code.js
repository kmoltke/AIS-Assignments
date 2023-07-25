
import { baseUrl, createReq, forgotReq, loginReq, tokenReq, genTokenReq } from "../api.js";
// import {genTokenReq} from "../api";

function emailForm () {
    $("#email").   css("display", "inline");
    $("#password").css("display", "none");
    $("#continue").css("display", "inline");
    $("#forgot").  css("display", "inline");
    $("#login").   css("display", "none");
    $("#create").  css("display", "none");
    $("#back").    css("display", "none");
}

function passwordForm () {
    $("#email").   css("display", "none");
    $("#password").css("display", "inline");
    $("#continue").css("display", "none");
    $("#forgot").  css("display", "none");
    $("#login").   css("display", "inline");
    $("#create").  css("display", "inline");
    $("#back").    css("display", "inline");
}

async function tokenForm() {
    console.log("I'm in tokenForm()");
    let loginSucess = await loginReq();
    if (loginSucess) {
        console.log("I'm in the if branch!");
        genTokenReq();
        $("#token").css("display", "inline");
        $("#sendToken").css("display", "inline")
        $("#email").css("display", "none");
        $("#password").css("display", "none");
        $("#continue").css("display", "none");
        $("#forgot").css("display", "none");
        $("#login").css("display", "none");
        $("#create").css("display", "none");
        $("#back").css("display", "inline");
    } else {
        console.log("I'm in the else branch :(");
    }
    // console.log("I'm in tokenForm()")
    // loginReq();
    // genTokenReq();
    // $("#token").css("display", "inline")
    // $("#sendToken").css("display", "inline")
    // $("#email").css("display", "none");
    // $("#password").css("display", "none");
    // $("#continue").css("display", "none");
    // $("#forgot").css("display", "none");
    // $("#login").css("display", "none");
    // $("#create").css("display", "none");
    // $("#back").css("display", "inline");

}

$(document).ready(function() {

    console.log("Setting continue behavior on continue button.");
    $("#continue").click( function(){ passwordForm(); } );

    console.log("Setting forgot behavior on forgot button.");
    $("#forgot").click( function(){ forgotReq(); } );
    
    console.log("Setting login behavior on login button.");
    $("#login").click( function(){ tokenForm(); } );

    console.log("Setting create behavior on create button.");
    $("#create").click( function(){ createReq(); } );

    console.log("Setting back behavior on back button.");
    $("#back").click( function(){ emailForm(); } );

    console.log("Setting token behavior on token button.");
    $("#sendToken").click( function(){ tokenReq(); } );

    console.log("Setting RETURN keypress behavior.");
    $("#token").keypress(function(event) {
        if (event.keyCode === 13) { /* pressing RETURN */
            $("#sendToken").click();
        }
    });

    $("#email").keypress(function(event) {
        if (event.keyCode === 13) { /* pressing RETURN */
            $("#continue").click(); 
        } 
    }); 
    $("#password").keypress(function(event) { 
        if (event.keyCode === 13) { /* pressing RETURN */
            $("#login").click(); 
        } 
    });

    emailForm();
});
