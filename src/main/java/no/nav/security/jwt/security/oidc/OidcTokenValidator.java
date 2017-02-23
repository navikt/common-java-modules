package no.nav.security.jwt.security.oidc;

import no.nav.security.jwt.security.jwks.JwksKeyHandler;
import no.nav.security.jwt.security.jwks.JwtHeader;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwx.JsonWebStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.security.Key;
import java.util.List;

public class OidcTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(OidcTokenValidator.class);

    private JwksKeyHandler jwks;
    private String token;

    private String subject;
    private long expSeconds;

    public OidcTokenValidator(JwksKeyHandler keyHandler, String token) {
        this.jwks = keyHandler;
        this.token = token;
    }

    public boolean tokenIsValid() {
        try {
            validate();
            return true;
        } catch (LoginException e) {
            return false;
        }
    }

    public OidcTokenValidator validate() throws LoginException {
        JwtHeader header;
        try {
            header = getHeader(token);
        } catch (InvalidJwtException e) {
            throw new LoginException("Invalid JWT " + e.getMessage());
        }
        Key key = jwks.getKey(header);
        if (key == null) {
            throw new LoginException(String.format("Jwt (%s) is not in jwks", header));
        }
        String expectedIssuer = System.getProperty("isso-issuer.url");
        if (expectedIssuer == null) {
            throw new LoginException("Expected issuer must be configured.");
        }
        String expectedAudience = System.getProperty("isso-rp-user.username");
        if (expectedAudience == null) {
            throw new LoginException("Expected audience must be configured.");
        }
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30) //TODO set to 0. Clocks should be synchronized.
                .setRequireSubject()
                .setExpectedIssuer(expectedIssuer)
                .setVerificationKey(key)
                .setExpectedAudience(expectedAudience)
                .build();

        try {
            JwtClaims claims = jwtConsumer.processToClaims(token);
            subject = claims.getSubject();
            expSeconds = claims.getExpirationTime().getValue();
            logger.info("JWT validation OK:" + subject);
        } catch (InvalidJwtException e) {
            logger.info("Feil ved validering av token.", e);
            throw new LoginException(e.toString());
        } catch (MalformedClaimException e) {
            throw new LoginException("Malformed claim: " + e.toString());
        }
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public long getExp() {
        return expSeconds;
    }

    private JwtHeader getHeader(String jwt) throws InvalidJwtException {
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setSkipAllDefaultValidators()
                .setRelaxVerificationKeyValidation()
                .setRelaxDecryptionKeyValidation()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();

        List<JsonWebStructure> objs = jwtConsumer.process(jwt).getJoseObjects();
        JsonWebStructure wstruct = objs.iterator().next();
        String kid = wstruct.getKeyIdHeaderValue();
        if (kid == null) {
            kid = "";
        }

        return new JwtHeader(kid, wstruct.getAlgorithmHeaderValue());
    }


}
