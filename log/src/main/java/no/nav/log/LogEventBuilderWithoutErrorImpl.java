package no.nav.log;

import net.logstash.logback.marker.MapEntriesAppendingMarker;
import no.nav.sbl.util.fn.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static no.nav.log.MaskedLoggingEvent.mask;

public class LogEventBuilderWithoutErrorImpl implements LogEventBuilderWithError, LogEventBuilderWithoutError {


    private final StringBuilder stringBuilder = new StringBuilder();
    private final Map<String, Object> map = new HashMap<>();
    private Throwable exception;

    @Override
    public LogEventBuilderWithoutError log(BiConsumer<Marker, String> callback) {
        callback.accept(new MapEntriesAppendingMarker(map), stringBuilder.toString());
        return this;
    }

    @Override
    public LogEventBuilderWithError log(TriConsumer<Marker, String, Throwable> callback) {
        callback.accept(new MapEntriesAppendingMarker(map), stringBuilder.toString(), exception);
        return null;
    }


    @Override
    public LogEventBuilderWithoutErrorImpl field(String fieldName, Object value) {
        String mask = value != null ? mask(value.toString()) : null;
        if (!map.isEmpty()) {
            stringBuilder.append(", ");
        }
        stringBuilder.append(fieldName);
        stringBuilder.append("=");
        stringBuilder.append(value);
        map.put(fieldName, mask);
        return this;
    }


    @Override
    public LogEventBuilderWithoutError logInfo(Logger logger) {
        BiConsumer<Marker, String> markerStringBiConsumer = logger::info;
        log(markerStringBiConsumer);
        return this;
    }

    @Override
    public LogEventBuilderWithError error(Throwable throwable) {
        field("errorMessage", throwable.getMessage());
        this.exception = throwable;
        return this;
    }

}
