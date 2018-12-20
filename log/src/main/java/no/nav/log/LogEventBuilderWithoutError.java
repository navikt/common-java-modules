package no.nav.log;

import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.function.BiConsumer;

public interface LogEventBuilderWithoutError extends LogEventBuilderBase {

    LogEventBuilderWithoutError log(BiConsumer<Marker, String> callback);
    LogEventBuilderWithoutError logInfo(Logger logger);

    LogEventBuilderWithError error(Throwable throwable);

    @Override
    LogEventBuilderWithoutError field(String fieldName, Object value);

}
