package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegisterData {

	public String username;
	public String password;

	public String email;
	
	public RegisterData() {}
	
	public RegisterData(String username, String password, String email) {
		this.username = username;
		this.password = password;
		this.email = email;
	}

	public boolean validRegistration() {
		if(username == null || password == null) {
			return false;
		}
	return true;
	}
}
