package no.nav.metrics;

public class MetodeEvent {

    public static Object eventForMetode(Metodekall metodekall, String eventNavn) throws Throwable {
        Event event = MetricsFactory.createEvent(eventNavn);

        try {
            Object resultat = metodekall.kallMetode();
            event.addFieldToReport("success", true);
            return resultat;
        } catch (Throwable throwable) {
            event.addFieldToReport("success", false);
            throw throwable;
        } finally {
            event.report();
        }
    }
}
