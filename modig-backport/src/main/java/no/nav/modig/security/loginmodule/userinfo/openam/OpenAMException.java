package no.nav.modig.security.loginmodule.userinfo.openam;

public class OpenAMException extends RuntimeException{
	
	private static final long serialVersionUID = 1L;
	
	public OpenAMException(String message){
		super(message);
	}
	public OpenAMException(Throwable cause){
		super(cause);
	}
	public OpenAMException(String message, Throwable cause){
		super(message, cause);
	}
}
