function sendRegisterAccount(){
    var username = document.getElementById("rUsername").value;
    var pwd = document.getElementById("rPwd").value;
    var email = document.getElementById("rEmail").value;
    var phone = document.getElementById("rPhone").value;
    var occupation = document.getElementById("rOccupation").value;
    var workPlace = document.getElementById("rWorkPlace").value;
    var nif = document.getElementById("rNif").value;
    var objectCompact = {username:username, password:pwd, email:email,
                        profileType: "private", phone:phone, occupation: occupation,
                        workPlace:workPlace, nif:nif, status:"inactive"};
    var JSONtransform = JSON.stringify(objectCompact);

    var xmlhttp = new XMLHttpRequest();

    xmlhttp.open("POST", "https://alien-container-379310.oa.r.appspot.com/rest/utils/register", true);
    xmlhttp.setRequestHeader("Content-type", "application/json");
    xmlhttp.send(JSONtransform);

    xmlhttp.onreadystatechange = function(){

        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
                alert("Registration successful. Activate to continue.")
            } else if (xmlhttp.status == 500) {
                alert("Transaction error. Possible invalid property value.");
            } else if (xmlhttp.status == 403) {
                alert("User already exists.");
            } else if (xmlhttp.status == 400){
                alert("Missing or wrong parameter.");
            } else {
                alert("An unexpected error has occurred.");
            }
        }
    }
}

function sendActivateUser(){
    var username = document.getElementById("actUsername").value;
    var pwd = document.getElementById("actPwd").value;

    var objectCompact = {username:username, password:pwd};
    var JSONtransform = JSON.stringify(objectCompact);

    var xmlhttp = new XMLHttpRequest();

    xmlhttp.open("POST", "https://alien-container-379310.oa.r.appspot.com/rest/utils/activate", true);
    xmlhttp.setRequestHeader("Content-type", "application/json");
    xmlhttp.send(JSONtransform);

    xmlhttp.onreadystatechange = function(){

        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
                alert("Activation successful. Log In to continue.")
            } else if (xmlhttp.status == 500) {
                alert("Transaction error. Possible invalid property value.");
            } else if (xmlhttp.status == 403) {
                alert("Wrong password or username.");
            } else {
                alert("An unexpected error has occurred.");
            }
        }
    }
}