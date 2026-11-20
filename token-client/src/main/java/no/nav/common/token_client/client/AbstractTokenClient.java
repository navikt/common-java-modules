package no.nav.common.token_client.client;

import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import java.net.URI;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.token_client.cache.TokenCache;

@Slf4j
public abstract class AbstractTokenClient {

    private static final Set<String> SAFE_ERROR_CODES = Set.of(
            "invalid_request",
            "invalid_client",
            "invalid_grant",
            "unauthorized_client",
            "unsupported_grant_type",
            "invalid_scope",
            "invalid_target",
            "access_denied",
            "server_error",
            "temporarily_unavailable");

    protected final String clientId;

    protected final URI tokenEndpoint;

    protected final String privateJwkKeyId;

    protected final JWSSigner assertionSigner;

    protected final TokenCache tokenCache;

    @SneakyThrows
    public AbstractTokenClient(String clientId, String tokenEndpointUrl, String privateJwk, TokenCache tokenCache) {
        this.clientId = clientId;
        this.tokenCache = tokenCache;

        tokenEndpoint = URI.create(tokenEndpointUrl);

        RSAKey rsaKey = RSAKey.parse(privateJwk);
        privateJwkKeyId = rsaKey.getKeyID();
        assertionSigner = new RSASSASigner(rsaKey);
    }

    protected static TokenResponse parseTokenResponse(HTTPResponse httpResponse, String operation) {
        TokenResponse response;
        try {
            response = TokenResponse.parse(httpResponse);
        } catch (ParseException e) {
            // Parserfeil kan inneholde responsdata og skal ikke inkluderes i unntaket.
            throw tokenFailure(operation, httpResponse.getStatusCode(), "invalid_response");
        }

        if (!response.indicatesSuccess()) {
            ErrorObject error = response.toErrorResponse().getErrorObject();
            String code = error == null ? null : error.getCode();
            String safeCode;
            if (code == null) {
                safeCode = "invalid_response";
            } else if (SAFE_ERROR_CODES.contains(code)) {
                safeCode = code;
            } else {
                safeCode = "unrecognized_error";
            }
            throw tokenFailure(operation, httpResponse.getStatusCode(), safeCode);
        }

        return response;
    }

    private static RuntimeException tokenFailure(String operation, int statusCode, String errorCode) {
        String message = operation + " failed: HTTP " + statusCode + ", error=" + errorCode;
        log.error(message);
        return new RuntimeException(message);
    }
}
