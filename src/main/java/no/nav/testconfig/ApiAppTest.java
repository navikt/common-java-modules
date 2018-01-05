package no.nav.testconfig;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;
import no.nav.sbl.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ch.qos.logback.classic.Level.INFO;
import static no.nav.dialogarena.config.util.Util.setProperty;
import static no.nav.metrics.MetricsFactory.DISABLE_METRICS_REPORT_KEY;

public class ApiAppTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAppTest.class);

    public static void setupTestContext() {
        getLoggerContext().getLogger("ROOT").iteratorForAppenders().forEachRemaining(ApiAppTest::simplifyConsoleAppender);
        LogUtils.setGlobalLogLevel(INFO);
        setProperty(DISABLE_METRICS_REPORT_KEY, Boolean.TRUE.toString());
        SSLTestUtils.disableCertificateChecks();
    }

    private static void simplifyConsoleAppender(Appender<ILoggingEvent> appender) {
        if (appender instanceof ConsoleAppender) {
            ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) appender;
            Encoder<ILoggingEvent> previousEncoder = consoleAppender.getEncoder();
            LayoutWrappingEncoder<ILoggingEvent> encoder = simpleEncoder(consoleAppender);
            consoleAppender.setEncoder(encoder);
            LOGGER.info("changed encoder for appender {}: {} -> {}",
                    appender,
                    previousEncoder,
                    encoder
            );
        }
    }

    private static LayoutWrappingEncoder<ILoggingEvent> simpleEncoder(ConsoleAppender<ILoggingEvent> consoleAppender) {
        TTLLLayout layout = new TTLLLayout();
        layout.setContext(consoleAppender.getContext());
        layout.start();

        LayoutWrappingEncoder<ILoggingEvent> wrappingEncoder = new LayoutWrappingEncoder<>();
        wrappingEncoder.setLayout(layout);
        wrappingEncoder.setContext(consoleAppender.getContext());
        wrappingEncoder.start();
        return wrappingEncoder;
    }

    private static LoggerContext getLoggerContext() {
        ContextBase contextBase = (ContextBase) LoggerFactory.getILoggerFactory();
        return (LoggerContext) contextBase;
    }

}
