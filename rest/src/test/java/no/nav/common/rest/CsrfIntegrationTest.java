package no.nav.common.rest;

import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPattern.everything;
import static no.nav.common.rest.RestUtils.CSRF_COOKIE_NAVN;
import static org.assertj.core.api.Assertions.assertThat;

public class CsrfIntegrationTest {

    private static final String REGULAR_COOKIE = "regular-cookie";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void smoketest() {
        givenThat(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("ok!"))
        );

        RestUtils.withClient(c -> c.target("http://localhost:" + wireMockRule.port())
                .request()
                .cookie(REGULAR_COOKIE,"someValue")
                .get(String.class)
        );

        List<LoggedRequest> requests = wireMockRule.findRequestsMatching(everything()).getRequests();
        assertThat(requests).hasSize(1);
        LoggedRequest loggedRequest = requests.get(0);

        Cookie csrfCookie = loggedRequest.getCookies().get(CSRF_COOKIE_NAVN);
        assertThat(csrfCookie).isNotNull();
        assertThat(loggedRequest.getHeader(CSRF_COOKIE_NAVN)).isEqualTo(csrfCookie.getValue());

        Cookie regularCookie = loggedRequest.getCookies().get(REGULAR_COOKIE);
        assertThat(regularCookie).isNotNull();
        assertThat(regularCookie.getValue()).isNotNull();
    }

}