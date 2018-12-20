package no.nav.log;

import no.nav.sbl.util.fn.TriConsumer;
import org.slf4j.Marker;

public interface LogEventBuilderWithError extends LogEventBuilderBase {

    LogEventBuilderWithError log(TriConsumer<Marker, String, Throwable> callback);

    @Override
    LogEventBuilderWithError field(String fieldName, Object value);

}
