package no.nav.brukerdialog.security.jaspic;


import no.nav.brukerdialog.tools.HostUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.SecureRandom;

public class AuthorizationRequestBuilder {

    private static final SecureRandom random = new SecureRandom();

    private final String scope = "openid";
    private boolean useKerberos = true;
    private String stateIndex;

    public AuthorizationRequestBuilder() {
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
        String clientId = System.getProperty("isso-rp-user.username");
        String state = stateIndex;
        String kerberosTrigger = useKerberos
                ? "session=winssochain&authIndexType=service&authIndexValue=winssochain&"
                : "";
        return String.format("%s/authorize?" + kerberosTrigger + "response_type=code&scope=%s&client_id=%s&state=%s&redirect_uri=%s",
                System.getProperty("isso-host.url"),
                scope,
                URLEncoder.encode(clientId, "UTF-8"),
                state,
                URLEncoder.encode(System.getProperty("oidc-redirect.url"), "UTF-8")
        );
    }


}
