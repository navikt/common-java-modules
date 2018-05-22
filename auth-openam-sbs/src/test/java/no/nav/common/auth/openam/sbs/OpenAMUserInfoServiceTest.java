package no.nav.common.auth.openam.sbs;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static no.nav.common.auth.SsoToken.Type.EKSTERN_OPENAM;
import static no.nav.common.auth.openam.sbs.OpenAMUserInfoService.BASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;


public class OpenAMUserInfoServiceTest {
    private String contentType = "Content-Type";
    private String json = "application/json";

    private OpenAMUserInfoService service;

    private WireMockConfiguration wireMockConfig = wireMockConfig().port(8000);// No-args constructor defaults to port 8080

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig);

    @Before
    public void setUp() throws Exception {
        service = new OpenAMUserInfoService(URI.create("http://localhost:8000" + BASE_PATH));
    }

    @Test
    public void getUserInfo__valid_token() {
        stubFor(get(urlMatching(BASE_PATH + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(200)
                        .withBody("{\"token\":{\"tokenId\":\"AQIC5wM2LY4Sfcy4eCp3R5V0YrY__qcn459QF4e2q2MesPA.*AAJTSQACMDIAAlMxAAIwMQ..*\"},\"roles\":[],\"attributes\":[{\"values\":[\"10026900250\"],\"name\":\"uid\"},{\"values\":[\"4\"],\"name\":\"SecurityLevel\"}]}")));

        Optional<Subject> validtoken = service.getUserInfo("sso-token");
        assertThat(validtoken).isNotEmpty();

        Subject subject = validtoken.get();
        assertThat(subject.getUid()).isEqualTo("10026900250");
        SsoToken ssoToken = subject.getSsoToken();
        assertThat(ssoToken.getType()).isEqualTo(EKSTERN_OPENAM);
        assertThat(ssoToken.getToken()).isEqualTo("sso-token");
        assertThat(subject.getIdentType()).isEqualTo(IdentType.EksternBruker);
    }

    @Test
    public void returnsFalseWhenStatusIsNotOK() {
        stubFor(get(urlMatching(BASE_PATH + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(401)
                        .withBody("{\"exception\":{\"name\":\"com.sun.identity.idsvcs.TokenExpired\",\"message\":\"Cannot retrieve Token.\"}}")));

        Optional<Subject> userInfo = service.getUserInfo("sso-token");

        assertThat(userInfo).isEmpty();
    }

    @Test
    public void returnsFalseWhenTokenNotOk() {
        stubFor(get(urlMatching(BASE_PATH + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(200)
                        .withBody("{\"exception\":{\"name\":\"com.sun.identity.idsvcs.TokenExpired\",\"message\":\"Cannot retrieve Token.\"}}")));

        Optional<Subject> userInfo = service.getUserInfo("sso-token");

        assertThat(userInfo).isEmpty();
    }

}
