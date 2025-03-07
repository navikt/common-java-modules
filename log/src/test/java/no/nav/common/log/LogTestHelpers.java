package no.nav.common.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter2;
import org.slf4j.LoggerFactory;

import java.net.URL;

import static java.util.Objects.requireNonNull;

public class LogTestHelpers {
    public static void loadLogbackConfig(String path) throws JoranException {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        URL configUrl = LogTestHelpers.class.getResource(path);
        requireNonNull(configUrl, "Config file not found: " + path);

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        configurator.doConfigure(configUrl);

        var statusPrinter = new StatusPrinter2();
        statusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    }

    public static void flushLogs() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
        loggerContext.start();
    }
}
