package no.nav.common.abac;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;

public class NimbusSubjectProvider implements SubjectProvider {

    private static final Logger logger = LoggerFactory.getLogger(NimbusSubjectProvider.class);

    @Override
    public String getSubjectFromToken(String idToken) {
        try {
            JWT jwtToken = JWTParser.parse(idToken);
            return jwtToken.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            logger.warn("Kunne ikke hente subject fra id token", e);
            return null;
        }
    }
}
