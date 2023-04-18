package no.nav.common.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
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
        return MaskedThrowableProxy.mask(iLoggingEvent.getThrowableProxy());
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
    public List<Marker> getMarkerList() {
        return iLoggingEvent.getMarkerList();
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return new MaskedMap(iLoggingEvent.getMDCPropertyMap());
    }

    @Override
    public Map<String, String> getMdc() {
        return new MaskedMap(iLoggingEvent.getMDCPropertyMap());
    }

    @Override
    public long getTimeStamp() {
        return iLoggingEvent.getTimeStamp();
    }

    @Override
    // TODO Workaround for bug https://github.com/logfellow/logstash-logback-encoder/issues/939 in logstash version 7.3
    public Instant getInstant() {
        if (ILoggingEvent.super.getInstant() == null) {
            return Instant.now();
        } else {
            return ILoggingEvent.super.getInstant();
        }
    }

    @Override
    public int getNanoseconds() {
        return iLoggingEvent.getNanoseconds();
    }

    @Override
    public long getSequenceNumber() {
        return iLoggingEvent.getSequenceNumber();
    }

    @Override
    public List<KeyValuePair> getKeyValuePairs() {
        return iLoggingEvent.getKeyValuePairs();
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
