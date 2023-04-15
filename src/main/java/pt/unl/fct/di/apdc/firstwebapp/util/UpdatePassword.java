package pt.unl.fct.di.apdc.firstwebapp.util;

public class UpdatePassword {

    public String username;
    public String oldPassword;

    public String newPassword;

    public UpdatePassword() {}

    public UpdatePassword(String username, String oldPassword, String newPassword) {
        this.username = username;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
