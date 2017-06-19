package no.nav.apiapp.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.ContextBase;
import no.nav.apiapp.ApiAppServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    public static void setGlobalLogLevel(Level newLevel) {
        ContextBase contextBase = (ContextBase) LoggerFactory.getILoggerFactory();
        LoggerContext loggerContext = (LoggerContext) contextBase;
        loggerContext.getLoggerList().forEach(l -> l.setLevel(newLevel));
    }

    public static void shutDownLogback() {
        LOGGER.info("shutDownLogback");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
    }
}
