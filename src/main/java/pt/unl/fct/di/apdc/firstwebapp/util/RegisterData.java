package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegisterData {

	public String username;
	public String password;

	public String email;

	public String profileType;

	public long phone;

	public String occupation;

	public String workPlace;

	public String nif;

	public String status;
	
	public RegisterData() {}
	
	public RegisterData(String username, String password, String email
	, String profileType, int phone, String occupation, String workPlace, String nif, String status) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.profileType = profileType;
		this.phone = phone;
		this.occupation = occupation;
		this.workPlace = workPlace;
		this.nif = nif;
		this.status = status;

	}

	public boolean validRegistration() {
		if(username == null || password == null || email == null) {
			return false;
		}
	return true;
	}
}
