package no.nav.sbl.featuretoggle.remote;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class MockServer {
    static final String MOCK_RESPONSE = "{\"aktivitetsplan\": {\"kvp\": false}}";

    static MockWebServer lagMockServer(long delay, String content) {
        return lagMockServer(delay, content, "");
    }

    static MockWebServer lagMockServer(long delay, String content, String etag) {
        boolean useEtag = etag != null && !etag.isEmpty();
        MockWebServer server = new MockWebServer();
        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(content);

        if (useEtag) {
            response.addHeader("etag", etag);
        }

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (delay > 0) {
                    Thread.sleep(delay);
                }
                if (useEtag) {
                    String matchRequest = request.getHeader("If-None-Match");
                    if (etag.equals(matchRequest)) {
                        return new MockResponse()
                                .setResponseCode(304)
                                .addHeader("etag", etag);
                    }
                }

                return response;
            }
        });

        return server;
    }
}
