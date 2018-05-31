package no.nav.sbl.rest;

import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPattern.everything;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static org.assertj.core.api.Assertions.assertThat;

public class CookieIntegrationTest {

    private static final String TEST_COOKIE_1 = "test-cookie-1";
    private static final String TEST_COOKIE_2 = "test-cookie-2";
    private static final String TEST_COOKIE_VALUE = "test-value";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    public void setup() {
        givenThat(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("ok!"))
        );
    }

    @Test
    public void cookie_object() {
        RestUtils.withClient(c -> c.target("http://localhost:" + wireMockRule.port())
                .request()
                .cookie(TEST_COOKIE_1, TEST_COOKIE_VALUE)
                .cookie(TEST_COOKIE_2, TEST_COOKIE_VALUE)
                .get(String.class)
        );

        assertCookies();
    }

    @Test
    public void cookie_string_and_object() {
        RestUtils.withClient(c -> c.target("http://localhost:" + wireMockRule.port())
                .request()
                .header(COOKIE, TEST_COOKIE_1 + "=" + TEST_COOKIE_VALUE)
                .cookie(TEST_COOKIE_2, TEST_COOKIE_VALUE)
                .get(String.class)
        );

        assertCookies();
    }

    private void assertCookies() {
        List<LoggedRequest> requests = wireMockRule.findRequestsMatching(everything()).getRequests();
        assertThat(requests).hasSize(1);
        LoggedRequest loggedRequest = requests.get(0);

        String header = loggedRequest.getHeader(COOKIE);
        assertThat(header).isEqualTo("test-cookie-1=test-value; test-cookie-2=test-value; NAV_CSRF_PROTECTION=csrf-token");

        Cookie cookie1 = loggedRequest.getCookies().get(TEST_COOKIE_1);
        assertThat(cookie1).isNotNull();
        assertThat(cookie1.getValue()).isEqualTo(TEST_COOKIE_VALUE);

        Cookie cookie2 = loggedRequest.getCookies().get(TEST_COOKIE_2);
        assertThat(cookie2).isNotNull();
        assertThat(cookie2.getValue()).isEqualTo(TEST_COOKIE_VALUE);
    }

}