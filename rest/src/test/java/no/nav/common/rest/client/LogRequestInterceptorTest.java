package no.nav.common.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static no.nav.common.test.wiremock.WireMockTestUtils.createWireMockExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import no.nav.common.log.MDCConstants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class LogRequestInterceptorTest {
    @RegisterExtension
    public WireMockExtension wireMockRule = createWireMockExtension();

    private ListAppender<ILoggingEvent> filterAppender;

    @SuppressWarnings("LoggerInitializedWithForeignClass")
    private final Logger filterLogger = (Logger) LoggerFactory.getLogger(LogRequestInterceptor.class);

    private Map<String, String> previousContextMap;

    @BeforeEach
    public void setUp() {
        previousContextMap = MDC.getCopyOfContextMap();
        MDC.clear();
        filterAppender = new ListAppender<>();
        filterAppender.start();
        filterLogger.addAppender(filterAppender);
    }

    @AfterEach
    public void tearDown() {
        filterLogger.detachAppender(filterAppender);
        filterAppender.stop();
        if (previousContextMap == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(previousContextMap);
        }
    }

    @Test
    public void filter_skal_logge_debug_hvis_suksess() throws IOException {
        OkHttpClient client = RestClient.baseClient();
        MDC.put(MDCConstants.MDC_CALL_ID, "CALL_ID");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.getPort())
                .build();

        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            assertThat(filterAppender.list).hasSize(1);
            assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getLevel() == Level.DEBUG);
            assertThat(filterAppender.list)
                    .allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().containsKey(MDCConstants.MDC_CALL_ID));
        }
    }

    @Test
    public void filter_skal_logge_warn_hvis_ikke_suksess() throws IOException {
        OkHttpClient client = RestClient.baseClient();
        MDC.put(MDCConstants.MDC_CALL_ID, "CALL_ID");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.getPort())
                .build();

        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(500);
            assertThat(filterAppender.list).hasSize(1);
            assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getLevel() == Level.WARN);
            assertThat(filterAppender.list)
                    .allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().containsKey(MDCConstants.MDC_CALL_ID));
        }
    }

    @Test
    public void filter_skal_logge_error_ved_exception() {
        OkHttpClient client = RestClient.baseClientBuilder()
                .addInterceptor(chain -> {
                    throw new UnknownHostException("Syntetisk nettverksfeil");
                })
                .build();
        MDC.put(MDCConstants.MDC_CALL_ID, "CALL_ID");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.getPort())
                .build();

        assertThrows(
                UnknownHostException.class,
                () -> client.newCall(request).execute().close());
        assertThat(filterAppender.list).hasSize(1);
        assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getLevel() == Level.ERROR);
        assertThat(filterAppender.list)
                .allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().containsKey(MDCConstants.MDC_CALL_ID));
    }

    @Test
    public void filter_skal_fjerne_query_params_fra_logmelding() throws IOException {
        OkHttpClient client = RestClient.baseClient();
        MDC.put(MDCConstants.MDC_CALL_ID, "CALL_ID");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.getPort() + "?queryparm=ignored")
                .build();

        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            assertThat(filterAppender.list).hasSize(1);
            assertThat(filterAppender.list)
                    .allMatch(iLoggingEvent -> iLoggingEvent.getMessage().contains("url=http://localhost"));
            assertThat(filterAppender.list)
                    .allMatch(iLoggingEvent -> !iLoggingEvent.getMessage().contains("queryparm"));
        }
    }
}
