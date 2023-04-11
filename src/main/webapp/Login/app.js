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
                alert("Login successful.")
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