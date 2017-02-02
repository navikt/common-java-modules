package no.nav.sbl.dialogarena.common.abac;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.runner.RunWith;

@RunWith(JMockit.class)
public final class HttpClientBuilderMock extends MockUp<HttpClientBuilder> {

    private final CloseableHttpClient client;

    public HttpClientBuilderMock(CloseableHttpClient closeableHttpClient) {
        this.client = closeableHttpClient;
    }

    @Mock
    public CloseableHttpClient build() {
        return client;
    }
}
