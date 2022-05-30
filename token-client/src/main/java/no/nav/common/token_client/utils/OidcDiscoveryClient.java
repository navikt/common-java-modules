package no.nav.common.token_client.utils;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import lombok.SneakyThrows;
import net.minidev.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class OidcDiscoveryClient {

    private final static int CONNECT_TIMEOUT = 10 * 1000;

    private final static int READ_TIMEOUT = 5 * 1000;

    @SneakyThrows
    public static OIDCProviderMetadata fetchDiscoveryMetadata(String discoveryUrl) {
        URL configURL = new URL(discoveryUrl);

        HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.GET, configURL);
        httpRequest.setConnectTimeout(CONNECT_TIMEOUT);
        httpRequest.setReadTimeout(READ_TIMEOUT);

        HTTPResponse httpResponse = httpRequest.send();

        if (httpResponse.getStatusCode() != 200) {
            throw new IOException("Couldn't download OpenID Provider metadata from " + configURL +
                    ": Status code " + httpResponse.getStatusCode());
        }

        JSONObject jsonObject = httpResponse.getContentAsJSONObject();

        return OIDCProviderMetadata.parse(jsonObject);
    }

}
