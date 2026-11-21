package no.nav.common.token_client.utils;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import java.io.IOException;
import java.net.URI;

import lombok.SneakyThrows;

public class OidcDiscoveryClient {
    private static final int CONNECT_TIMEOUT = 10 * 1000;
    private static final int READ_TIMEOUT = 5 * 1000;

    @SneakyThrows
    public static OIDCProviderMetadata fetchDiscoveryMetadata(String discoveryUrl) {
        var configURL = URI.create(discoveryUrl).toURL();

        var httpRequest = new HTTPRequest(HTTPRequest.Method.GET, configURL);
        httpRequest.setConnectTimeout(CONNECT_TIMEOUT);
        httpRequest.setReadTimeout(READ_TIMEOUT);

        HTTPResponse httpResponse = httpRequest.send();

        if (httpResponse.getStatusCode() != 200) {
            throw new IOException("Couldn't download OpenID Provider metadata from " + configURL + ": Status code "
                + httpResponse.getStatusCode());
        }

        var jsonObject = httpResponse.getBodyAsJSONObject();

        return OIDCProviderMetadata.parse(jsonObject);
    }
}
