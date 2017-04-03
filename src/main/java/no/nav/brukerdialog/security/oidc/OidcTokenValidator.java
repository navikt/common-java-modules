package no.nav.brukerdialog.security.oidc;

import no.nav.brukerdialog.security.jwks.DefaultJwksKeyHandler;
import no.nav.brukerdialog.security.jwks.JwksKeyHandler;
import no.nav.brukerdialog.security.jwks.JwtHeader;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwx.JsonWebStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.List;

public class OidcTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(OidcTokenValidator.class);

    private JwksKeyHandler jwks;

    public static class OidcTokenValidatorResult {
        private final boolean isValid;
        private final String errorMessage;
        private final String subject;
        private final long expSeconds;

        private OidcTokenValidatorResult(boolean isValid, String errorMessage, String subject, long expSeconds) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.subject = subject;
            this.expSeconds = expSeconds;
        }

        public static OidcTokenValidatorResult invalid(String errorMessage) {
            return new OidcTokenValidatorResult(false, errorMessage, null, 0);
        }

        public static OidcTokenValidatorResult valid(String subject, long expSeconds) {
            return new OidcTokenValidatorResult(true, null, subject, expSeconds);
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            if (isValid) {
                throw new IllegalArgumentException("Can't get error message from valid token");
            }
            return errorMessage;
        }

        public String getSubject() {
            if (!isValid) {
                throw new IllegalArgumentException("Can't get claims from an invalid token");
            }
            return subject;
        }

        public long getExpSeconds() {
            if (!isValid) {
                throw new IllegalArgumentException("Can't get claims from an invalid token");
            }
            return expSeconds;
        }
    }

    public OidcTokenValidator() {
        this.jwks = DefaultJwksKeyHandler.INSTANCE;
    }

    public OidcTokenValidator(JwksKeyHandler keyHandler) {
        this.jwks = keyHandler;
    }

    public OidcTokenValidatorResult validate(String token) {
        if (token == null) {
            return OidcTokenValidatorResult.invalid("Missing token (token was null)");
        }
        JwtHeader header;
        try {
            header = getHeader(token);
        } catch (InvalidJwtException e) {
            return OidcTokenValidatorResult.invalid("Invalid OIDC " + e.getMessage());
        }
        Key key = jwks.getKey(header);
        if (key == null) {
            return OidcTokenValidatorResult.invalid(String.format("Jwt (%s) is not in jwks", header));
        }
        String expectedIssuer = System.getProperty("isso-issuer.url");
        if (expectedIssuer == null) {
            return OidcTokenValidatorResult.invalid("Expected issuer must be configured.");
        }
        String expectedAudience = System.getProperty("isso-rp-user.username");
        if (expectedAudience == null) {
            return OidcTokenValidatorResult.invalid("Expected audience must be configured.");
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
            logger.debug("OIDC validation OK:" + claims.getSubject());
            return OidcTokenValidatorResult.valid(claims.getSubject(), claims.getExpirationTime().getValue());
        } catch (InvalidJwtException e) {
            logger.info("Feil ved validering av token.", e);
            return OidcTokenValidatorResult.invalid(e.toString());
        } catch (MalformedClaimException e) {
            return OidcTokenValidatorResult.invalid("Malformed claim: " + e.toString());
        }
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

        List<JsonWebStructure> jsonObjects = jwtConsumer.process(jwt).getJoseObjects();
        JsonWebStructure wstruct = jsonObjects.iterator().next();
        String kid = wstruct.getKeyIdHeaderValue();
        if (kid == null) {
            kid = "";
        }
        return new JwtHeader(kid, wstruct.getAlgorithmHeaderValue());
    }

}
