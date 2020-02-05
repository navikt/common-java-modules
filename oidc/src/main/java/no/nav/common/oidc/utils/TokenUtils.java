package no.nav.common.oidc.utils;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.brukerdialog.security.domain.IdentType;

import java.text.ParseException;

import static no.nav.common.oidc.Constants.AAD_NAV_IDENT_CLAIM;

public class TokenUtils {

    public static String getUid(JWT token, IdentType identType) throws ParseException {
        JWTClaimsSet claimsSet = token.getJWTClaimsSet();
        String subject = claimsSet.getSubject();

        if (identType == IdentType.InternBruker) {
            String navIdent = claimsSet.getStringClaim(AAD_NAV_IDENT_CLAIM);
            return navIdent != null
                    ? navIdent
                    : subject;
        }

        return subject;
    }

}
