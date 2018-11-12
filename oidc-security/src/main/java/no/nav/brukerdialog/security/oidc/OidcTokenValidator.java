package no.nav.brukerdialog.security.oidc;

import no.nav.brukerdialog.security.jwks.JwtHeader;
import no.nav.brukerdialog.security.jwks.CacheMissAction;
import no.nav.brukerdialog.security.oidc.provider.OidcProvider;
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

import static no.nav.brukerdialog.security.jwks.CacheMissAction.REFRESH;

public class OidcTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(OidcTokenValidator.class);
    private static final int ALLOWED_CLOCK_SKEW_IN_SECONDS = 30;

    public OidcTokenValidatorResult validate(String token, OidcProvider oidcProvider) {
        return validate(token, oidcProvider, REFRESH);
    }

    public OidcTokenValidatorResult validate(
            String token,
            OidcProvider oidcProvider,
            CacheMissAction cacheMissAction
    ) {
        if (token == null) {
            return OidcTokenValidatorResult.invalid("Missing token (token was null)");
        }
        JwtHeader header;
        try {
            header = getHeader(token);
        } catch (InvalidJwtException e) {
            return OidcTokenValidatorResult.invalid("Invalid OIDC " + e.getMessage());
        }
        Key verificationKey = oidcProvider.getVerificationKey(header, cacheMissAction).orElse(null);
        if (verificationKey == null) {
            return OidcTokenValidatorResult.invalid(String.format("Jwt (%s) is not in jwks", header));
        }
        String issoExpectedTokenIssuer = oidcProvider.getExpectedIssuer();
        if (issoExpectedTokenIssuer == null) {
            return OidcTokenValidatorResult.invalid("Expected issuer must be configured.");
        }

        String expectedAud = oidcProvider.getExpectedAudience(token);

        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(ALLOWED_CLOCK_SKEW_IN_SECONDS)
                .setRequireSubject()
                .setExpectedIssuer(issoExpectedTokenIssuer)
                .setExpectedAudience(false,expectedAud) //requireAudienceClaim til false slik at det funker om openAM fjerner aud fra token.
                .setVerificationKey(verificationKey)
                .build();

        try {
            JwtClaims claims = jwtConsumer.processToClaims(token);
            logger.debug("OIDC validation OK:" + claims.getSubject());
            return OidcTokenValidatorResult.valid(claims);
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
