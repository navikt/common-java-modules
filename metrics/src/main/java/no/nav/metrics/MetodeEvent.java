package no.nav.metrics;

import java.util.Map;
import java.util.function.Function;

public class MetodeEvent {

    public static Object eventForMetode(Metodekall metodekall, String eventNavn) throws Throwable {
        return eventForMetode(metodekall, eventNavn, null);
    }

    public static Object eventForMetode(Metodekall metodekall, String eventNavn, Map<String, String> verdier) throws Throwable {
        return eventForMetode(metodekall, eventNavn, verdier, MetricsFactory::createEvent);
    }

    static Object eventForMetode(Metodekall metodekall,
                                 String eventNavn,
                                 Map<String, String> verdier,
                                 Function<String, Event> createEvent) throws Throwable {
        Event event = createEvent.apply(eventNavn);
        if (verdier != null) {
            for (Map.Entry<String, String> verdi : verdier.entrySet()) {
                event.addFieldToReport(verdi.getKey(), verdi.getValue());
            }
        }

        try {
            return metodekall.kallMetode();
        } catch (RuntimeException | Error unchecked) {
            event.setFailed();
            event.addFieldToReport("checkedException", false);
            throw unchecked;
        } catch (Throwable checked) {
            event.setFailed();
            event.addFieldToReport("checkedException", true);
            throw checked;
        } finally {
            event.report();
        }
    }
}
