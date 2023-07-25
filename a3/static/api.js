
var baseUrl  = "http://localhost:5000";
var email = document.cookie.split("=")[1];
var balance;
var text;

// function createReq() {
//     console.log("Sending 'create user' request to server. email=" + $('#email').val() + ", password=" + $('#password').val());
//     $.ajax({
//         method: "GET",
//         url: baseUrl + "/api/create",
//         data: { email: $('#email').val() , password: $('#password').val()}
//     }).done( function (response) {
//         console.log("Success: 'create user'.");
//         text = response.text;
//         console.log(text);
//         $("#response").html("<p>" + text + "</p>");
//     }).fail( function (jqXHR, textStatus, errorThrown) {
//         console.log("Error: 'create user'.");
//         text = jqXHR.responseJSON.text;
//         console.log(text);
//         $("#response").html("<p>" + text + "</p>");
//     });
// }

function createReq() {
    return new Promise((resolve, reject) => {
        console.log("Sending 'create user' request to server. email=" + $('#email').val() + ", password=" + $('#password').val());
        $.ajax({
            method: "GET",
            url: baseUrl + "/api/create",
            data: { email: $('#email').val() , password: $('#password').val()}
        }).done( function (response) {
            console.log("Success: 'create user'.");
            text = response.text;
            console.log(text);
            $("#response").html("<p>" + text + "</p>");
            resolve(true);
        }).fail( function (jqXHR, textStatus, errorThrown) {
            console.log("Error: 'create user'.");
            text = jqXHR.responseJSON.text;
            console.log(text);
            $("#response").html("<p>" + text + "</p>");
            resolve(false);
        });
    });
}

function forgotReq() {
    console.log("Sending 'forgot password' request to server. email=" + $('#email').val());
    $.ajax({
        method: "GET",
        url: baseUrl + "/api/forgot",
        data: { email: $('#email').val() }
    }).done( function (response) {        
        console.log("Success: 'forgot password'.");
        text = response.text;
        console.log(text);
        $("#response").html("<p>" + text + "</p>");
    }).fail( function (jqXHR, textStatus, errorThrown) {
        console.log("Error: 'forgot password'.");
        text = jqXHR.responseJSON.text;
        console.log(text);
        $("#response").html("<p>" + text + "</p>");
    });
}

function loginReq() {
    return new Promise((resolve, reject) => {
        console.log("Sending 'login' request to server. email=" + $('#email').val() + ", password=" + $('#password').val());
        $.ajax({
            method: "GET",
            url: baseUrl + "/api/login",
            data: { email: $('#email').val() , password: $('#password').val() }
        }).done(function(response) {
            console.log("Success: 'login'.");
            text = response.text;
            console.log(text);
            resolve(true);
        }).fail(function(jqXHR, textStatus, errorThrown) {
            console.log("Error: 'login'.");
            text = jqXHR.responseJSON.text;
            console.log(text);
            $("#response").html("<p>" + text + "</p>");
            resolve(false);
        });
    });
}


function genTokenReq() {
    console.log("Sending generate token request to server. email=" + $('#email').val());
    $.ajax({
        method: "GET",
        url: baseUrl + "/api/genToken",
        data: {email: $('#email').val()}
    }).done(function (response) {
        console.log("Success: 'Generate token'.");
        text = response.text;
        console.log(text);
        $("#response").html("<p>" + text + "</p>");
    }).fail(function (jqXHR, textStatus, errorThrown) {
        console.log("Error: 'Generate token'.");
        text = jqXHR.responseJSON.text;
        $("#response").html("<p>" + text + "</p>");
    })
}

function tokenReq() {
    console.log("Sending 'token' request to server. email="+ $('#token').val());
    $.ajax({
        method: "GET",
        url: baseUrl + "/api/auth",
        data: {email: $('#email').val(), token: $('#token').val() }
    }).done( function (response) {
        console.log("Success: 'authentication'.");
        text = response.text;
        console.log(text);
        window.location.href = "http://localhost:5000/menu";
    }).fail( function (jqXHR, textStatus, errorThrown) {
        console.log("Error: 'authentication'.");
        text = jqXHR.responseJSON.text;
        console.log(text);

        if (jqXHR.status === 419) {
            genTokenReq();
            $("#response").html("<p>The token has expired. A new token has been sent to your email.</p>");
        } else {
            $("#response").html("<p>" + text + "</p>");
        }
    });
}

function logoutReq() {
    console.log("Sending 'logout' request to server. email=" + email);
    $.ajax({
        method: "GET",
        url: baseUrl + "/api/logout"
    }).done( function (response) {
        console.log("Success 'logout' request.");
        text = response.text;
        console.log(text);
        $("#response").html("<p>" + text + "</p>");
    }).fail( function (jqXHR, textStatus, errorThrown) {
        console.log("Error: 'logout'.");
        var text = jqXHR.responseJSON.text;
        console.log(text);
        $("#response").html("<p>" + text + "</p>");
    });
    window.location.href = baseUrl + "/login";
}

function balanceReq() {
    console.log("Sending 'balance' request to server. email=" + email);
    $.ajax({
        method: "GET",
        url: baseUrl + "/api/balance"
    }).done( function (response) {        
        console.log("Success 'balance' request.");
        balance = response.balance;
        text    = response.text;
        console.log(text + " Balance:" + balance);
        $("#balance").html(balance);
    }).fail( function (jqXHR, textStatus, errorThrown) {
        console.log("Error: 'balance'.");
        text = jqXHR.responseJSON.text;
        console.log(text);
        window.location.href = baseUrl + "/login";
    });
}

function sendReq() {
    console.log("Sending 'send' request to server. email=" + email + ", amount=" + $("#balance"));
    $.ajax({
        method: "GET",
        url: baseUrl + "/api/send",
        data: { amount: $('#amount').val() , to: $('#to').val()}
    }).done( function (response) {
        console.log("Success 'send' request.");
        text    = response.text;
        console.log(text);
        $("#response").html("<p>" + text + "</p>");
    }).fail( function (jqXHR, textStatus, errorThrown) {
        console.log("Error  'send' request.");
        text = jqXHR.responseJSON.text;
        console.log(text);
        $("#response").html("<p>" + text + "</p>");
    });
    balanceReq();
}

function depositReq() {
    console.log("Sending 'deposit' request to server. email=" + email + ", cardnumber=" + $("#cardnumber") + ", amount=" + $("#amount"));
    $.ajax({
        method: "GET",
        url: baseUrl + "/api/deposit",
        data: { cardnumber: $('#cardnumber').val() , amount: $('#amount').val()}
    }).done( function (response) {
        console.log("Success 'deposit' request.");
        text    = response.text;
        console.log(text);
        $("#response").html("<p>" + text + "</p>");
    }).fail( function (jqXHR, textStatus, errorThrown) {
        console.log("Error: 'deposit'.");
        text = jqXHR.responseJSON.text;
        console.log(text);
        $("#response").html("<p>" + text + "</p>");
    });
    balanceReq();
}

function withdrawReq() {
    console.log("Sending 'withdraw' request to server. email=" + email + ", cardnumber=" + $("#cardnumber") + ", amount=" + $("#amount"));
    $.ajax({
        method: "GET",
        url: baseUrl + "/api/withdraw",
        data: { cardnumber: $('#cardnumber').val() , amount: $('#amount').val()}
    }).done( function (response) {
        console.log("Success: 'withdraw'.");
        text    = response.text;
        console.log(text);
        $("#response").html("<p>" + text + "</p>");
    }).fail( function (jqXHR, textStatus, errorThrown) {
        console.log("Error: 'withdraw'.");
        text = jqXHR.responseJSON.text;
        console.log(text);
        $("#response").html("<p>" + text + "</p>");
    });
    balanceReq();
}

export { baseUrl, email, balance, forgotReq, createReq, loginReq, logoutReq, balanceReq, sendReq, depositReq, withdrawReq, tokenReq, genTokenReq };
