package no.nav.common.sts;

import com.nimbusds.jwt.JWT;
import no.nav.common.auth.utils.TokenUtils;

public class SystemUserTokenUtils {

	private final static int MINIMUM_TIME_TO_EXPIRE_BEFORE_REFRESH = 60 * 1000; // 1 minute

	public static boolean tokenNeedsRefresh(JWT accessToken) {
		return accessToken == null || TokenUtils.expiresWithin(accessToken, MINIMUM_TIME_TO_EXPIRE_BEFORE_REFRESH);
	}

}
