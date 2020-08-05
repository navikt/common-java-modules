package no.nav.common.auth.oidc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import no.nav.common.auth.oidc.discovery.OidcDiscoveryConfiguration;
import no.nav.common.auth.oidc.discovery.OidcDiscoveryConfigurationClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

public class OidcTokenValidator {

    private final static JWSAlgorithm JWS_ALGORITHM = JWSAlgorithm.RS256;

    // Mapping between clientId and validator
    private final HashMap<String, IDTokenValidator> validators;

    private final String issuer;

    public OidcTokenValidator(String oidcDiscoveryUrl, List<String> clientIds) {
        OidcDiscoveryConfigurationClient client = new OidcDiscoveryConfigurationClient();
        OidcDiscoveryConfiguration config = client.fetchDiscoveryConfiguration(oidcDiscoveryUrl);

        issuer = config.issuer;
        validators = new HashMap<>();

        clientIds.forEach((id) -> validators.put(id, createValidator(config.issuer, config.jwksUri, JWS_ALGORITHM, id)));
    }

    public IDTokenClaimsSet validate(JWT idToken) throws BadJOSEException, JOSEException, ParseException {
        String clientId;
        JWTClaimsSet claims = idToken.getJWTClaimsSet();
        List<String> tokenAudiences = claims.getAudience();

        /*
         Tokens that have more than 1 audience should also have the AZP claim.
         Nimbus will check that tokens with multiple audiences will have an AZP claim that matches one of the audiences.
         */
        if (tokenAudiences.size() > 1) {
            clientId = claims.getStringClaim("azp");
        } else {
            clientId = tokenAudiences.get(0);
        }

        IDTokenValidator validator = validators.get(clientId);

        if (validator == null) {
            throw new RuntimeException("Could not find validator for audience " + clientId);
        }

        return validator.validate(idToken, null);
    }

    public IDTokenClaimsSet validate(String token) throws ParseException, JOSEException, BadJOSEException {
        return validate(JWTParser.parse(token));
    }

    public String getIssuer() {
        return issuer;
    }

    private IDTokenValidator createValidator(String issuerUrl, String jwksUrl, JWSAlgorithm algorithm, String clientId) {
        Issuer issuer = new Issuer(issuerUrl);
        ClientID clientID = new ClientID(clientId);
        try {
            return new IDTokenValidator(issuer, clientID, algorithm, new URL(jwksUrl));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid jwks URL " + jwksUrl);
        }
    }

}
