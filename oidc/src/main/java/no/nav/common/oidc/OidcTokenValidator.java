package no.nav.common.oidc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

public class OidcTokenValidator {

    private final static JWSAlgorithm JWS_ALGORITHM = JWSAlgorithm.RS256;

    private final IDTokenValidator validator;

    public OidcTokenValidator(String oidcDiscoveryUrl, String clientId) {
        OidcDiscoveryConfigurationClient client = new OidcDiscoveryConfigurationClient();
        OidcDiscoveryConfiguration config = client.fetchDiscoveryConfiguration(oidcDiscoveryUrl);
        validator = createValidator(config.issuer, config.jwksUri, JWS_ALGORITHM, clientId);
    }

    public OidcTokenValidator(String issuerUrl, String jwksUrl, JWSAlgorithm algorithm, String clientId) {
        validator = createValidator(issuerUrl, jwksUrl, algorithm, clientId);
    }

    public IDTokenClaimsSet validate(JWT idToken) throws BadJOSEException, JOSEException {
        return validator.validate(idToken, null);
    }

    public IDTokenClaimsSet validate(String token) throws ParseException, JOSEException, BadJOSEException {
        return validate(JWTParser.parse(token));
    }

    private IDTokenValidator createValidator(String issuerUrl, String jwksUrl, JWSAlgorithm algorithm, String clientId) {
        Issuer issuer = new Issuer(issuerUrl);
        ClientID clientID = new ClientID(clientId);
        try {
            return new IDTokenValidator(issuer, clientID, algorithm, new URL(jwksUrl));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
