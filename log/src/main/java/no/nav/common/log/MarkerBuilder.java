package no.nav.common.log;

import net.logstash.logback.marker.MapEntriesAppendingMarker;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class MarkerBuilder {

    private final StringBuilder stringBuilder = new StringBuilder();
    private final Map<String, Object> map = new HashMap<>();

    public MarkerBuilder log(BiConsumer<Marker, String> callback) {
        callback.accept(new MapEntriesAppendingMarker(map), stringBuilder.toString());
        return this;
    }

    public MarkerBuilder field(String fieldName, Object value) {
        String mask = value != null ? MaskedLoggingEvent.mask(value.toString()) : null;
        if (!map.isEmpty()) {
            stringBuilder.append(", ");
        }
        stringBuilder.append(fieldName);
        stringBuilder.append("=");
        stringBuilder.append(value);
        map.put(fieldName, mask);
        return this;
    }

    public void logInfo(Logger logger) {
        log(logger::info);
    }

}
