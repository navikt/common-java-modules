package no.nav.brukerdialog.security.jaspic;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.SecureRandom;

public class AuthorizationRequestBuilder {

    private static final SecureRandom random = new SecureRandom();

    private final String scope = "openid";
    private final String issoHostUrl;
    private final String clientId;
    private final String redirectUrl;
    private boolean useKerberos = true;
    private String stateIndex;

    public AuthorizationRequestBuilder() {
        this(AuthorizationRequestBuilderConfig.resolveFromSystemProperties());
    }

    public AuthorizationRequestBuilder(AuthorizationRequestBuilderConfig authorizationRequestBuilderConfig) {
        this.issoHostUrl = authorizationRequestBuilderConfig.getIssoHostUrl();
        this.clientId = authorizationRequestBuilderConfig.getClientId();
        this.redirectUrl = authorizationRequestBuilderConfig.getRedirectUrl();

        byte bytes[] = new byte[20];
        random.nextBytes(bytes);
        stateIndex = "state_" + new BigInteger(1, bytes).toString(16);
    }

    public AuthorizationRequestBuilder ikkeBrukKerberos() {
        useKerberos = false;
        return this;
    }

    public String getStateIndex() {
        return stateIndex;
    }

    public String buildRedirectString() throws UnsupportedEncodingException {
        String state = stateIndex;
        String kerberosTrigger = useKerberos
                ? "session=winssochain&authIndexType=service&authIndexValue=winssochain&"
                : "";
        return String.format("%s/authorize?" + kerberosTrigger + "response_type=code&scope=%s&client_id=%s&state=%s&redirect_uri=%s",
                issoHostUrl,
                scope,
                URLEncoder.encode(clientId, "UTF-8"),
                state,
                URLEncoder.encode(redirectUrl, "UTF-8")
        );
    }
}