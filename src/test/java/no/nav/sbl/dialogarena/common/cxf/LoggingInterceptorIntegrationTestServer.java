package no.nav.sbl.dialogarena.common.cxf;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import no.nav.sbl.rest.RestUtils;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class LoggingInterceptorIntegrationTestServer extends JettyTestServer {

    final StringBuilder builder = new StringBuilder();

    @Before
    public void setUp() throws Exception {
        LoggerContext iLoggerFactory = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = iLoggerFactory.getLogger("ROOT");
        AppenderBase<ILoggingEvent> appender = new AppenderBase<ILoggingEvent>() {
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
        builder.delete(0, builder.length());
    }

    @Test
    public void skal_fjerne_cookie_i_header() throws Exception {
        String url = startCxfServer(Aktoer_v2PortType.class, true, true);
        sendRequest(url);
        String logline = builder.toString();
        boolean containsCookie = logline.contains("Cookie");
        boolean containsLoggerTest = logline.contains("LoggeTest");
        Assert.assertFalse(containsCookie);
        Assert.assertTrue(containsLoggerTest);
    }

    @Test
    public void skal_logge_cookie_i_header() throws Exception {

        String url = startCxfServer(Aktoer_v2PortType.class, true, false);
        sendRequest(url);
        String logline = builder.toString();
        boolean containsCookie = logline.contains("Cookie");
        boolean containsLoggerTest = logline.contains("LoggeTest");
        Assert.assertTrue(containsCookie);
        Assert.assertTrue(containsLoggerTest);

    }

    private void sendRequest(String url) {
        RestUtils.withClient(client -> client.target(url)
                .queryParam("wsdl")
                .request()
                .cookie("ID_token", "SUPERHEMMELIG")
                .header("LoggeTest", "LOGGETESTER")
                .get());
    }
}