package no.nav.common.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.ContextBase;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.common.utils.fn.UnsafeSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

import java.util.List;
import java.util.Map;

public class LogUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    public static final String ROOT = "ROOT";

    public static void runWithMDCContext(Map<String, String> mdcContext, UnsafeRunnable runnable) {
        runWithMDCContext(mdcContext, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T runWithMDCContext(Map<String, String> mdcContext, UnsafeSupplier<T> supplier) {
        MDCAdapter mdcAdapter = MDC.getMDCAdapter();
        Map<String, String> prevContext = MDC.getCopyOfContextMap();

        try {
            if (mdcAdapter != null && mdcContext != null) {
                mdcAdapter.setContextMap(mdcContext);
            }

            return supplier.get();
        } finally {
            if (mdcAdapter != null) {
                if (prevContext != null) {
                    mdcAdapter.setContextMap(prevContext);
                } else {
                    mdcAdapter.clear();
                }
            }
        }
    }

    public static void setGlobalLogLevel(Level newLevel) {
        LOGGER.info("global log level: {}", newLevel);
        LoggerContext loggerContext = getLoggerContext();
        loggerContext.getLoggerList().forEach(l -> l.setLevel(newLevel));
    }

    public static void shutDownLogback() {
        LOGGER.info("shutDownLogback");
        LoggerContext loggerContext = getLoggerContext();
        loggerContext.stop();
    }

    private static LoggerContext getLoggerContext() {
        ContextBase contextBase = (ContextBase) LoggerFactory.getILoggerFactory();
        return (LoggerContext) contextBase;
    }

    public static List<ch.qos.logback.classic.Logger> getAllLoggers() {
        LoggerContext loggerContext = getLoggerContext();
        return loggerContext.getLoggerList();
    }

    public static Level getRootLevel() {
        LoggerContext loggerContext = getLoggerContext();
        return loggerContext.getLogger(ROOT).getLevel();
    }

    public static MarkerBuilder buildMarker() {
        return new MarkerBuilder();
    }

}
