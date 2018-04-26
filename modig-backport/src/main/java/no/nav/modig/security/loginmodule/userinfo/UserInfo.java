package no.nav.modig.security.loginmodule.userinfo;


public class UserInfo {
	
	private String uid;
	private int authLevel;
	
	public UserInfo(String uid, int authLevel){
		this.uid = uid;
		this.authLevel = authLevel;
	}
	
	public String getUid() {
		return uid;
	}

	
	public int getAuthLevel() {
		return authLevel;
	}
	
	@Override
	public String toString(){
		return "[uid="+uid+", authLevel="+authLevel+"]";
	}
}
