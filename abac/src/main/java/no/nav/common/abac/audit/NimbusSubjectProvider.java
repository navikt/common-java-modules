package no.nav.common.abac.audit;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;

import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;
import static no.nav.common.auth.Constants.ID_PORTEN_PID_CLAIM;

public class NimbusSubjectProvider implements SubjectProvider {

    private static final Logger logger = LoggerFactory.getLogger(NimbusSubjectProvider.class);

    @Override
    public String getSubjectFromToken(String idToken) {
        try {
            JWT jwtToken = JWTParser.parse(idToken);
            JWTClaimsSet claimsSet = jwtToken.getJWTClaimsSet();

            if (claimsSet.getClaim(AAD_NAV_IDENT_CLAIM) != null) {
                return claimsSet.getStringClaim(AAD_NAV_IDENT_CLAIM);
            } else if (claimsSet.getClaim(ID_PORTEN_PID_CLAIM) != null) {
                return claimsSet.getStringClaim(ID_PORTEN_PID_CLAIM);
            } else {
                return jwtToken.getJWTClaimsSet().getSubject();
            }
        } catch (ParseException e) {
            logger.warn("Kunne ikke hente subject fra id token", e);
            return null;
        }
    }
}
