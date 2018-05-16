package no.nav.modig.security.loginmodule.userinfo;

public abstract class AbstractUserInfoService implements UserInfoService {

	@Override
	public abstract UserInfo getUserInfo(String subjectId);
	
}
