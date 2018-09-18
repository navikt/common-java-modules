package no.nav.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import org.slf4j.Marker;

import java.util.HashMap;
import java.util.Map;

public class MaskedLoggingEvent implements ILoggingEvent {
    private final ILoggingEvent iLoggingEvent;

    MaskedLoggingEvent(ILoggingEvent iLoggingEvent) {
        this.iLoggingEvent = iLoggingEvent;
    }

    public static String mask(String string) {
        return string != null ? string.replaceAll("(^|\\W)\\d{11}(?=$|\\W)", "$1***********") : null;
    }

    @Override
    public String getThreadName() {
        return iLoggingEvent.getThreadName();
    }

    @Override
    public Level getLevel() {
        return iLoggingEvent.getLevel();
    }

    @Override
    public String getMessage() {
        return iLoggingEvent.getMessage();
    }

    @Override
    public Object[] getArgumentArray() {
        return iLoggingEvent.getArgumentArray();
    }

    @Override
    public String getFormattedMessage() {
        return mask(iLoggingEvent.getFormattedMessage());
    }


    @Override
    public String getLoggerName() {
        return iLoggingEvent.getLoggerName();
    }

    @Override
    public LoggerContextVO getLoggerContextVO() {
        return iLoggingEvent.getLoggerContextVO();
    }

    @Override
    public IThrowableProxy getThrowableProxy() {
        return iLoggingEvent.getThrowableProxy();
    }

    @Override
    public StackTraceElement[] getCallerData() {
        return iLoggingEvent.getCallerData();
    }

    @Override
    public boolean hasCallerData() {
        return iLoggingEvent.hasCallerData();
    }

    @Override
    public Marker getMarker() {
        return iLoggingEvent.getMarker();
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return new MaskedMap(iLoggingEvent.getMDCPropertyMap());
    }

    @Override
    public Map<String, String> getMdc() {
        return new MaskedMap(iLoggingEvent.getMdc());
    }

    @Override
    public long getTimeStamp() {
        return iLoggingEvent.getTimeStamp();
    }

    @Override
    public void prepareForDeferredProcessing() {
        iLoggingEvent.prepareForDeferredProcessing();
    }


    private static class MaskedMap extends HashMap<String, String> {
        private MaskedMap(Map<String, String> map) {
            map.forEach((k, v) -> MaskedMap.this.put(k, mask(v)));
        }
    }

}
