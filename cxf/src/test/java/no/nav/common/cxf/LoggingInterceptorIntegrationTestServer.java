package no.nav.common.cxf;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import no.nav.common.cxf.jetty.JettyTestServer;
import no.nav.common.rest.RestClient;
import okhttp3.*;
import org.apache.servicemix.examples.cxf.HelloWorld;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static java.lang.System.setProperty;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LoggingInterceptorIntegrationTestServer extends JettyTestServer {

    final StringBuilder builder = new StringBuilder();
    AppenderBase<ILoggingEvent> appender;

    @Before
    public void setUp() throws Exception {
        LoggerContext iLoggerFactory = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = iLoggerFactory.getLogger("ROOT");
        appender = new AppenderBase<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent iLoggingEvent) {
                if (iLoggingEvent.getMessage().contains("Headers")) {
                    builder.append(iLoggingEvent.getMessage());
                }
            }
        };
        appender.start();
        rootLogger.addAppender(appender);
    }

    @After
    public void tearDown() throws Exception {
        appender.stop();
    }

    @Test
    public void skal_fjerne_cookie_i_header() throws Exception {
        setProperty("no.nav.common.cxf.cxfendpoint.logging.logg-tokeninheader", "false");
        String url = startCxfServer(HelloWorld.class);
        sendRequest(url);
        String logline = builder.toString();
        assertThat(logline).doesNotContain("Cookie");
        assertThat(logline).contains("LoggeTest");
    }

    @Test
    public void skal_logge_cookie_i_header() throws Exception {
        setProperty("no.nav.common.cxf.cxfendpoint.logging.logg-tokeninheader", "true");
        String url = startCxfServer(HelloWorld.class);
        sendRequest(url);
        String logline = builder.toString();
        assertThat(logline).contains("Cookie");
        assertThat(logline).contains("LoggeTest");
    }

    private void sendRequest(String url) {
       OkHttpClient client = RestClient.baseClientBuilder().cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {

            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                return Collections.singletonList(
                    new Cookie.Builder()
                            .domain("test.local")
                            .name("ID_token")
                            .value("SUPERHEMMELIG")
                            .httpOnly()
                            .secure()
                            .build()
                );
            }
        }).build();

        Request request = new Request.Builder()
                .url(url + "?wsdl")
                .header("LoggeTest", "LOGGETESTER")
                .build();

        try (Response response = client.newCall(request).execute()) {

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}