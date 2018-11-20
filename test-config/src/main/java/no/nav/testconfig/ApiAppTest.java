package no.nav.testconfig;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUserCertificate;
import no.nav.sbl.dialogarena.test.WebProxyConfigurator;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;
import no.nav.sbl.util.LogUtils;
import no.nav.validation.ValidationUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import static ch.qos.logback.classic.Level.INFO;
import static no.nav.metrics.MetricsConfig.SENSU_CLIENT_HOST;
import static no.nav.metrics.MetricsConfig.SENSU_CLIENT_PORT;
import static no.nav.sbl.util.EnvironmentUtils.*;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;

public class ApiAppTest {

    static {
        System.setProperty("SERVICE_CALLS_HOME", "target/log/accesslog");
        System.setProperty("APP_LOG_HOME", "target/log");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAppTest.class);

    @Value
    @Builder
    public static class Config {
        @NotEmpty
        public String applicationName;
    }

    @SneakyThrows
    public static void setupTestContext(Config config) {
        ValidationUtils.validate(config);
       // getLoggerContext().getLogger("ROOT").iteratorForAppenders().forEachRemaining(ApiAppTest::simplifyConsoleAppender);
        LogUtils.setGlobalLogLevel(INFO);

        SensuServerThread sensuServerThread = new SensuServerThread();
        sensuServerThread.start();
        setProperty(SENSU_CLIENT_HOST, "localhost", PUBLIC);
        setProperty(SENSU_CLIENT_PORT, Integer.toString(sensuServerThread.getPort()), PUBLIC);

        setProperty(APP_NAME_PROPERTY_NAME, config.applicationName, PUBLIC);
        setProperty(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME, FasitUtils.getDefaultEnvironment(), PUBLIC);
        SSLTestUtils.disableCertificateChecks();

        if (isUtviklerImage()) {
            WebProxyConfigurator.setupWebProxy();
//            setupNavTrustStore();
        }
    }

    @SneakyThrows
    private static void setupNavTrustStore() {
        LOGGER.info("Setting up NAV Truststore");
        ServiceUserCertificate navTrustStore = FasitUtils.getServiceUserCertificate("nav_truststore", FasitUtils.getDefaultEnvironmentClass());
        File navTrustStoreFile = File.createTempFile("nav_truststore", ".jks");
        FileUtils.writeByteArrayToFile(navTrustStoreFile,navTrustStore.getKeystore());
        setProperty("javax.net.ssl.trustStore", navTrustStoreFile.getAbsolutePath(), PUBLIC);
        setProperty("javax.net.ssl.trustStorePassword", navTrustStore.getKeystorepassword(), SECRET);
    }


    public static boolean isUtviklerImage() {
        try {
            boolean reachable = InetAddress.getByName("fasit.adeo.no").isReachable(5000);
            if (reachable) {
                LOGGER.info("Assuming access to fasit");
            }
            return reachable;
        } catch (IOException e) {
            LOGGER.info("Access check to fasit threw exception, assuming local dev environment");
            return false;
        }
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
