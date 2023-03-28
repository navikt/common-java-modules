package no.nav.common.rest.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.log.MDCConstants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.UnknownHostException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

public class LogRequestInterceptorTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private ListAppender<ILoggingEvent> filterAppender;
    private final Logger filterLogger = (Logger) LoggerFactory.getLogger(LogRequestInterceptor.class);

    @Before
    public void setUp() {
        filterAppender = new ListAppender<>();
        filterAppender.start();
        filterLogger.addAppender(filterAppender);
    }

    @After
    public void tearDown() {
        filterLogger.detachAppender(filterAppender);
    }

    @Test
    public void filter_skal_logge_debug_hvis_suksess() throws IOException {
        OkHttpClient client = RestClient.baseClient();
        MDC.put(MDCConstants.MDC_CALL_ID, "CALL_ID");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.port())
                .build();

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            assertThat(filterAppender.list).hasSize(1);
            assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getLevel() == Level.DEBUG);
            assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().containsKey(MDCConstants.MDC_CALL_ID));
        }
    }

    @Test
    public void filter_skal_logge_warn_hvis_ikke_suksess() throws IOException {
        OkHttpClient client = RestClient.baseClient();
        MDC.put(MDCConstants.MDC_CALL_ID, "CALL_ID");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.port())
                .build();

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(500)));

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(500);
            assertThat(filterAppender.list).hasSize(1);
            assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getLevel() == Level.WARN);
            assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().containsKey(MDCConstants.MDC_CALL_ID));
        }
    }

    @Test
    public void filter_skal_logge_error_ved_exception() throws IOException {
        OkHttpClient client = RestClient.baseClient();
        MDC.put(MDCConstants.MDC_CALL_ID, "CALL_ID");

        Request request = new Request.Builder()
                .url("http://illegalurl:9999")
                .build();


        //noinspection EmptyTryBlock
        try (Response ignored = client.newCall(request).execute()) {
        } catch (UnknownHostException e) {
            assertThat(filterAppender.list).hasSize(1);
            assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getLevel() == Level.ERROR);
            assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().containsKey(MDCConstants.MDC_CALL_ID));
        }
    }

    @Test
    public void filter_skal_fjerne_query_params_fra_logmelding() throws IOException {
        OkHttpClient client = RestClient.baseClient();
        MDC.put(MDCConstants.MDC_CALL_ID, "CALL_ID");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.port() + "?queryparm=ignored")
                .build();

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            assertThat(filterAppender.list).hasSize(1);
            assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getMessage().contains("url=http://localhost"));
            assertThat(filterAppender.list).allMatch(iLoggingEvent -> ! iLoggingEvent.getMessage().contains("queryparm"));
        }
    }
}