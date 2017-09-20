package no.nav.sbl.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.ContextBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    public static void setGlobalLogLevel(Level newLevel) {
        LOGGER.info("global log level: {}", newLevel);
        LoggerContext loggerContext = getLoggerContext();
        loggerContext.getLoggerList().forEach(l -> l.setLevel(newLevel));
    }

    @SuppressWarnings("unused")
    public static void shutDownLogback() {
        LOGGER.info("shutDownLogback");
        getLoggerContext().stop();
    }

    private static LoggerContext getLoggerContext() {
        ContextBase contextBase = (ContextBase) LoggerFactory.getILoggerFactory();
        return (LoggerContext) contextBase;
    }

}
