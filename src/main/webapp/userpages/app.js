//login
function sendLoginAccount(){
    var username = document.getElementById("logUsername").value;
    var pwd = document.getElementById("logPwd").value;
    var objectCompact = {username:username, password:pwd};
    var JSONtransform = JSON.stringify(objectCompact);
    var xmlhttp = new XMLHttpRequest();

    xmlhttp.open("POST", "https://alien-container-379310.oa.r.appspot.com/rest/utils/login", true);
    xmlhttp.setRequestHeader("Content-type", "application/json");
    xmlhttp.send(JSONtransform);

    xmlhttp.onreadystatechange = function(){

        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
                //Mandar para user page
                var newObj = JSON.parse(xmlhttp.responseText);

                sessionStorage.setItem("username", newObj.username);
                sessionStorage.setItem("token", newObj.tokenID);
                sessionStorage.setItem("creationDate", newObj.creationData);
                sessionStorage.setItem("expirationDate", newObj.expirationTime);

                window.location.assign('userpage.html');
            } else if (xmlhttp.status == 500) {
                alert("Transaction error. Possible invalid property value.");
            } else if (xmlhttp.status == 403) {
                alert("Wrong password or username.");
            } else if (xmlhttp.status == 400){
                alert("User does not exist.");
            } else {
                alert("An unexpected error has occurred.");
            }
        }
    }
}
window.onload = function showTokenFromUser() {
    if(sessionStorage.getItem("username") != null){
        document.getElementById("usershow").innerHTML = "Username: " + sessionStorage.getItem("username");
        document.getElementById("tokenshow").innerHTML = "TokenID: " + sessionStorage.getItem("token");
        document.getElementById("dateshow").innerHTML = "Valid from " + sessionStorage.getItem("creationDate") + " to " + sessionStorage.getItem("expirationDate");
    }
}
//Delete
function sendDeleteAccount(){
    var username = sessionStorage.getItem("username");
    var pwd = document.getElementById("delPwd").value;

    var objectCompact = {username:username, password:pwd};
    var JSONtransform = JSON.stringify(objectCompact);

    var xmlhttp = new XMLHttpRequest();

    xmlhttp.open("POST", "https://alien-container-379310.oa.r.appspot.com/rest/utils/deleteUser", true);
    xmlhttp.setRequestHeader("Content-type", "application/json");
    xmlhttp.send(JSONtransform);

    xmlhttp.onreadystatechange = function(){

        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
                window.location.assign('../index.html');
            } else if (xmlhttp.status == 400) {
                alert("User does not exist");
            } else if (xmlhttp.status == 403) {
                alert("Wrong password or username.");
            } else if (xmlhttp.status == 500){
                alert("Internal server error");
            } else {
                alert("An unexpected error has occurred.");
            }
        }
    }
}

//Update Password
function sendUpdatePassword(){
    var username = sessionStorage.getItem("username");
    var oldPwd = document.getElementById("oldPwd").value;
    var newPwd = document.getElementById("newPwd").value;
    var newPwdCheck = document.getElementById("newPwdCheck").value;

    if(newPwd!=newPwdCheck) {
        alert("New passwords do not match");
    }
    else {
        var objectCompact = {username: username, oldPassword: oldPwd, newPassword: newPwd};
        var JSONtransform = JSON.stringify(objectCompact);

        var xmlhttp = new XMLHttpRequest();

        xmlhttp.open("POST", "https://alien-container-379310.oa.r.appspot.com/rest/utils/updatePassword", true);
        xmlhttp.setRequestHeader("Content-type", "application/json");
        xmlhttp.send(JSONtransform);

        xmlhttp.onreadystatechange = function(){
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status == 200) {
                    alert("Password updated.");
                } else if (xmlhttp.status == 400) {
                    alert("User does not exist");
                } else if (xmlhttp.status == 403) {
                    alert("Wrong current password.");
                } else if (xmlhttp.status == 500){
                    alert("Internal server error");
                } else {
                    alert("An unexpected error has occurred.");
                }
            }
        }

    }



}

function sendLogout(){
    var username = sessionStorage.getItem("username");

    var objectCompact = {username:username};
    var JSONtransform = JSON.stringify(objectCompact);

    var xmlhttp = new XMLHttpRequest();

    xmlhttp.open("POST", "https://alien-container-379310.oa.r.appspot.com/rest/utils/logout", true);
    xmlhttp.setRequestHeader("Content-type", "application/json");
    xmlhttp.send(JSONtransform);

    xmlhttp.onreadystatechange = function(){

        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
                alert("Logout successful")
                window.location.assign('../index.html');
            } else if (xmlhttp.status == 400) {
                alert("User does not exist");
            } else if (xmlhttp.status == 408) {
                alert("Session timed out.");
                window.location.assign('../userpages/login.html')
            } else if (xmlhttp.status == 500){
                alert("Internal server error");
            } else {
                alert("An unexpected error has occurred.");
            }
        }
    }
}

function sendUpdate(){
    var username = sessionStorage.getItem("username");
    var profileType = document.getElementById("uPhone").value;
    var phone = document.getElementById("uPhone").value.toString();
    var occupation = document.getElementById("uOccupation").value;
    var workPlace = document.getElementById("uWorkPlace").value;
    var nif = document.getElementById("uNif").value;
    var status = document.getElementById("uStatus").value;

    var objectCompact = {username:username, profileType:profileType, phone:phone,
                         occupation:occupation, workPlace:workPlace, nif:nif, status:status};
    var JSONtransform = JSON.stringify(objectCompact);

    var xmlhttp = new XMLHttpRequest();

    xmlhttp.open("POST", "https://alien-container-379310.oa.r.appspot.com/rest/utils/changeSelf", true);
    xmlhttp.setRequestHeader("Content-type", "application/json");
    xmlhttp.send(JSONtransform);

    xmlhttp.onreadystatechange = function(){

        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
                alert("Logout successful")
                window.location.assign('../index.html');
            } else if (xmlhttp.status == 400) {
                alert("User does not exist");
            } else if (xmlhttp.status == 408) {
                alert("Session timed out.");
                window.location.assign('../userpages/login.html')
            } else if (xmlhttp.status == 500){
                alert("Internal server error");
            } else {
                alert("An unexpected error has occurred.");
            }
        }
    }
}