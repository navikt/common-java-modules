package no.nav.apiapp.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.ContextBase;
import org.slf4j.LoggerFactory;

public class LogUtils {

    public static void setGlobalLogLevel(Level newLevel) {
        ContextBase contextBase = (ContextBase) LoggerFactory.getILoggerFactory();
        LoggerContext loggerContext = (LoggerContext) contextBase;
        loggerContext.getLoggerList().forEach(l -> l.setLevel(newLevel));
    }
}
