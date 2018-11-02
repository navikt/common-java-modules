package no.nav.common.auth.openam.sbs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static no.nav.common.auth.SsoToken.Type.EKSTERN_OPENAM;
import static no.nav.common.auth.openam.sbs.OpenAMUserInfoService.BASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OpenAMUserInfoServiceTest {
    private String contentType = "Content-Type";
    private String json = "application/json";

    private OpenAMUserInfoService service;

    private WireMockConfiguration wireMockConfig = wireMockConfig().port(8000);// No-args constructor defaults to port 8080

    private Logger targetClassLogger = (Logger) LoggerFactory.getLogger(OpenAMUserInfoService.class);
    private Appender<ILoggingEvent> mockedLogAppender = mock(Appender.class);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig);

    @Before
    public void setUp() throws Exception {
        service = new OpenAMUserInfoService(URI.create("http://localhost:8000" + BASE_PATH));
    }

    @Test
    public void getUserInfo__invalid_session_because_of_expired_token() {
        beginLogCapture();

        String body = "{\"exception\":{\"message\":\"Invalid session ID.AQIC5wM2LY4Sfcy4eCp3R5V0YrY__qcn459QF4e2q2MesPA.*AAJTSQACMDIAAlMxAAIwMQ..*\",\"name\":\"com.sun.identity.idsvcs.TokenExpired\"}}";
        stubFor(get(urlMatching(BASE_PATH + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(401)
                        .withBody(body)));

        Optional<Subject> validtoken = service.convertTokenToSubject("sso-token");
        assertThat(validtoken).isEmpty();

        verifyLevelOfSingleLogEvent(Level.INFO);
        endLogCapture();
    }

    @Test
    public void getUserInfo__unknown_error() {
        beginLogCapture();

        String body = "{Internal Server Error}";
        stubFor(get(urlMatching(BASE_PATH + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(500)
                        .withBody(body)));

        Optional<Subject> validtoken = service.convertTokenToSubject("sso-token");
        assertThat(validtoken).isEmpty();

        verifyLevelOfSingleLogEvent(Level.ERROR);
        endLogCapture();
    }

    @Test
    public void getUserInfo__valid_token() {
        stubFor(get(urlMatching(BASE_PATH + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(200)
                        .withBody("{\"token\":{\"tokenId\":\"AQIC5wM2LY4Sfcy4eCp3R5V0YrY__qcn459QF4e2q2MesPA.*AAJTSQACMDIAAlMxAAIwMQ..*\"},\"roles\":[],\"attributes\":[{\"values\":[\"10026900250\"],\"name\":\"uid\"},{\"values\":[\"4\"],\"name\":\"SecurityLevel\"}]}")));

        Optional<Subject> validtoken = service.convertTokenToSubject("sso-token");
        assertThat(validtoken).isNotEmpty();

        Subject subject = validtoken.get();
        assertThat(subject.getUid()).isEqualTo("10026900250");
        SsoToken ssoToken = subject.getSsoToken();
        assertThat(ssoToken.getType()).isEqualTo(EKSTERN_OPENAM);
        assertThat(ssoToken.getToken()).isEqualTo("sso-token");
        assertThat(subject.getIdentType()).isEqualTo(IdentType.EksternBruker);
    }

    @Test
    public void getUserInfo__ignore_empty_attributes() {
        stubFor(get(urlMatching(BASE_PATH + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(200)
                        .withBody("{\"attributes\":[{\"values\":[],\"name\":\"a\"},{\"values\":[\"b\"],\"name\":\"b\"}]}")));

        Optional<Map<String, String>> userInfo = service.getUserInfo("sso-token", Arrays.asList("a", "b"));
        assertThat(userInfo.get())
                .hasSize(1)
                .containsEntry("b","b");
    }

    @Test
    public void getUserInfo__null_or_empty_token() {
        assertThat(service.getUserInfo(null, Arrays.asList("a", "b", "c"))).isEmpty();
        assertThat(service.getUserInfo("", Arrays.asList("a", "b", "c"))).isEmpty();
    }

    @Test
    public void returnsFalseWhenStatusIsNotOK() {
        stubFor(get(urlMatching(BASE_PATH + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(401)
                        .withBody("{\"exception\":{\"name\":\"com.sun.identity.idsvcs.TokenExpired\",\"message\":\"Cannot retrieve Token.\"}}")));

        Optional<Subject> userInfo = service.convertTokenToSubject("sso-token");

        assertThat(userInfo).isEmpty();
    }

    @Test
    public void returnsFalseWhenTokenNotOk() {
        stubFor(get(urlMatching(BASE_PATH + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(200)
                        .withBody("{\"exception\":{\"name\":\"com.sun.identity.idsvcs.TokenExpired\",\"message\":\"Cannot retrieve Token.\"}}")));

        Optional<Subject> userInfo = service.convertTokenToSubject("sso-token");

        assertThat(userInfo).isEmpty();
    }

    private void beginLogCapture() {
        targetClassLogger.addAppender(mockedLogAppender);
    }

    private void endLogCapture() {
        targetClassLogger.detachAppender(mockedLogAppender);
    }

    private void verifyLevelOfSingleLogEvent(Level error) {
        ArgumentCaptor<LoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
        verify(mockedLogAppender).doAppend(loggingEventCaptor.capture());
        assertThat(loggingEventCaptor.getValue().getLevel()).isEqualTo(error);
    }

}
