var global_jwt_expiration_date = null;
var global_login_sequence_in_progress = false;
var global_operations_waiting_for_jwt = [];
var global_jwt_update_listener;
var global_jwt_claims = null;
var openAmHost = erDev() ? 'https://modapp-t11.adeo.no' : "";
var getJwtAsCookieUrl = 'https://A34DUVW25929.devillo.no:9590/veilarbveileder/tjenester/login';

function onStartup() {
    getProtected("js/bundle.js", initApplication);
}

function initApplication(status, source){
    if (status === 200){
        appendStatus("Received application from server");

        // add js-source to web page
        var appScript = document.createElement("script");
        appScript.setAttribute("type", "text/javascript");
        appScript.innerHTML = source;
        document.head.appendChild(appScript);

        // start application by calling initApp of the downloaded application
    } else {
        reportError("Could not load application, status from server was: " + status);
    }
}

/**
 Use this function to communicate with services which require authentication with JWT.
 This function is responsible for renewing JWT if necessarily.
 */
function getProtected(url, callback) {
    var operation = function() {
        doGet(url, callback);
    }

    if (jwtExpirationImminent()) {
        appendStatus("JWT expires imminently: Blocking operation: " + url);
        global_operations_waiting_for_jwt.push(operation);
        if (!global_login_sequence_in_progress) {
            appendStatus("JWT expires imminently: fetching new JWT");
            startLoginSequence();
        }
    } else {
        operation();
        if (jwtExpirationSoon() && !global_login_sequence_in_progress){
            appendStatus("JWT expires soon, renewing");
            startLoginSequence();
        }
    }
}

function startLoginSequence(){
    global_login_sequence_in_progress = true;
    spnegoLogin();
}

function startUsernamePasswordLoginSequence() {
    usernamePasswordLogin();
}

function spnegoLogin(){
    appendStatus("Starting SPNEGO login");
    doPost(openAmHost + "/openam/json/authenticate?session=winssochain&authIndexType=service&authIndexValue=winssochain",
        {},
        null,
        function spnegoSuccess(status, responsetext) {
            if (status === 200) {
                var data = JSON.parse(responsetext);
                var openAmToken = data.tokenId;
                if (openAmToken) {
                    appendStatus("Obtained OpenAM token");
                    openIdConnectLogin(openAmHost, openAmToken);
                } else {
                    handleSpnegoLoginFailed("response did not contain tokenid")
                }
            } else {
                handleSpnegoLoginFailed("HTTP response code was " + response.status)
            }
        });
}

function handleSpnegoLoginFailed(reason) {
    reportError("SPNEGO login failed: " + reason + ". Using fallback to username/password login.");
    enableUsernamePasswordLoginForm();
}

function enableUsernamePasswordLoginForm() {
    var div = document.getElementById("usernamePasswordForm");
    div.style.display = "block";
}

function disableUsernamePasswordLoginForm() {
    var div = document.getElementById("usernamePasswordForm");
    div.style.display = "none";

    //clear password
    var passwordField = document.getElementById("password");
    passwordField.value = "";
}

function usernamePasswordLogin() {
    appendStatus("Starting username and password login");
    doPost(openAmHost + "/openam/json/authenticate",
        {
            "X-OpenAM-Username": document.getElementById("username").value,
            "X-OpenAM-Password": document.getElementById("password").value
        },
        null,
        function (status, responsetext) {
            if (status == "200") {
                var data = JSON.parse(responsetext);
                var openAmToken = data.tokenId;
                if (openAmToken) {
                    appendStatus("Obtained OpenAM token");
                    disableUsernamePasswordLoginForm();
                    openIdConnectLogin(openAmHost, openAmToken);
                } else {
                    handleUsernamePasswordLoginFailed("response did not contain tokenId");
                }
            } else {
                handleUsernamePasswordLoginFailed("HTTP response code was " + status);
            }
        });
}

function handleUsernamePasswordLoginFailed(reason){
    enableUsernamePasswordLoginForm();
    loginFailed("could not obtain openAM token: " + reason);
}

function openIdConnectLogin(openAmHost, openAmToken){
    appendStatus("OpenIDConnectStepup login");
    var nonce = Math.random().toString();
    var payload = JSON.stringify({
        "input_token_state": {
            "token_type": "OPENAM",
            "session_id": openAmToken
        },
        "output_token_state": {
            "scope" : "openid",
            "token_type": "OPENIDCONNECT",
            "nonce": nonce,
            "allow_access": true
        }
    });
    doPost(openAmHost + "/openam/rest-sts/stspki?_action=translate",
        {"Content-type" : "application/json;charset=UTF-8"},
        payload,
        function (status, responsetext) {
            if (status !== 200) {
                loginFailed("could not obtain JSON web token - HTTP response code was " + status);
                return;
            }
            if (!responsetext){
                loginFailed("could not obtain JSON web token - response did not contain token");
                return;
            }

            var data = JSON.parse(responsetext)
            var jwt = data.issued_token;
            var claimedNonce = getClaims(jwt).nonce;

            if (claimedNonce !== nonce) {
                loginFailed("Nonce in JWT did not match expected nonce " + nonce);
                return;
            }

            appendStatus("Obtained JSON web token (JWT)");
            setJwtCookie(jwt);
        });
}

function loginFailed(reason){
    reportError(reason);
    global_login_sequence_in_progress = false;
}

function setJwtCookie(jwt){
    doGetWithJwt(getJwtAsCookieUrl, jwt,
        function (status, reponsetext) {
            if (status === 200) {
                appendStatus("Authorization cookie set in browser.")
                global_jwt_claims = getClaims(jwt);
                appendStatus("Claims from JWT: " + JSON.stringify(global_jwt_claims));
                global_jwt_expiration_date = getExpirationDate(global_jwt_claims);
                global_login_sequence_in_progress = false;
                runEnqueuedOperations();
                notifyClaimsListener();
            } else {
                loginFailed("could not set autorization cookie - HTTP response code was " + status);
            }
        });
}

function runEnqueuedOperations(){
    var operation;
    while (global_operations_waiting_for_jwt.length > 0) {
        operation = global_operations_waiting_for_jwt.shift();
        appendStatus("Running enqueued operations " + operation );
        operation();
    }
}

function notifyClaimsListener(claims){
    if (global_jwt_update_listener) {
        global_jwt_update_listener(claims);
    }
}

///
/// JWT utils
///

function jwtExpirationImminent(){
    var week = 7 * 24 * 3600 * 1000;
    var second =  1000;
    /*
     recommended value is 2 minutes for production use
     jwtExpiresWithinMs(2 *  60 * second);
     */
    return jwtExpiresWithinMs(week - 60 * second);
}

function jwtExpirationSoon(){
    var week = 7 * 24 * 3600 * 1000;
    var second =  1000;
    /*
     recommended value is 5 minutes for production use
     jwtExpiresWithinMs(5 *  60 * second);
     */
    return jwtExpiresWithinMs(week - 20 * second);
}

function jwtExpiresWithinMs(limitMillis){
    if (global_jwt_expiration_date === null){
        return true;
    }
    var millisUntilExpire = global_jwt_expiration_date - new Date();
    return millisUntilExpire < limitMillis;
}


function getClaims(jwt) {
    // jwt består av tre base64-kodede deler: header, payload og signatur. Disse er adskilt med punktum.
    // claims finnes i payload
    var base64payload = jwt.split("\.")[1];

    //trailing "=" i base64-kodede deler er fjernet i jwt, må legge til dette for å dekode
    //dekoding krever at lengden der delelig med 4, må legge til "=" inntil dette stemmer
    while (base64payload.length % 4 !== 0) {
        base64payload += "=";
    }
    var payload = atob(base64payload);
    return JSON.parse(payload);
}

function getExpirationDate(claims) {
    return new Date(claims.exp*1000);
}

//
//   AJAX utils
//


function doGet(url, callback){
    var request = new XMLHttpRequest();
    request.open("GET", url);
    request.onreadystatechange = function(){
        var DONE = 4;
        if (request.readyState === DONE){
            callback(request.status, request.responseText);
        }
    };
    request.send(null);
}

function doGetWithJwt(url, jwt, callback){
    var request = new XMLHttpRequest();
    request.open("GET", url);
    request.withCredentials= true;
    request.setRequestHeader("Authorization", "Bearer " + jwt);
    request.onreadystatechange = function(){
        var DONE = 4;
        if (request.readyState === DONE){
            callback(request.status, request.responseText);
        }
    };
    request.send(null);
}

function doPost(url, headers, payload, callback){
    var request = new XMLHttpRequest();
    request.open("POST", url);

    var key;
    for (key in headers){
        if (headers.hasOwnProperty(key)) {
            request.setRequestHeader(key, headers[key]);
        }
    }
    request.onreadystatechange = function(){
        var DONE = 4;
        if (request.readyState == DONE){
            callback(request.status, request.responseText);
        }
    };
    request.send(payload);
}


//
// logging/status
//

function reportError(error) {
    appendStatus("ERROR:" + error);
}

function appendStatus(message) {
    var d = new Date();
    var div = document.getElementById("statusPane");
    var text = d.toISOString() + " " + message;
    div.innerHTML = text + "<br />" + div.innerHTML;
    console.log(text);
}
function erDev() {
    const url = window.location.href;
    return url.includes('debug=true') || url.includes('devillo.no:') || url.includes('localhost:');
}

