function sendRegisterAccount(){
    var username = document.getElementById("rUsername").value;
    var pwd = document.getElementById("rPwd").value;
    var email = document.getElementById("rEmail").value;

    var objectCompact = {username:username, password:pwd, email:email};
    var JSONtransform = JSON.stringify(objectCompact);

    var xmlhttp = new XMLHttpRequest();

    xmlhttp.open("POST", "https://alien-container-379310.oa.r.appspot.com/rest/utils/register", true);
    xmlhttp.setRequestHeader("Content-type", "application/json");
    xmlhttp.send(JSONtransform);

    xmlhttp.onreadystatechange = function(){

        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
                alert("Registration successful. Log In to continue.")
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
