package pt.unl.fct.di.apdc.firstwebapp.util;

public class SelfChange {


    public String username;

    public String profileType;

    public String phone;

    public String occupation;

    public String workPlace;

    public String nif;

    public String status;

    public SelfChange() {}

    public SelfChange(String username, String profileType, String phone, String occupation,
                      String workPlace, String nif, String status) {
        this.username = username;
        this.profileType = profileType;
        this.phone = phone;
        this.occupation = occupation;
        this.workPlace = workPlace;
        this.nif = nif;
        this.status = status;
    }
}
