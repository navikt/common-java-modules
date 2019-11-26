package no.nav.metrics;

import java.util.function.Function;

public class MetodeTimer {

    static Object timeMetode(Metodekall metodekall, String timerNavn, Function<String, Timer> createTimer) throws Throwable {
        Timer timer = createTimer.apply(timerNavn);

        try {
            timer.start();
            return metodekall.kallMetode();
        } catch (RuntimeException | Error unchecked) {
            timer.setFailed();
            timer.addFieldToReport("checkedException", false);
            throw unchecked;
        } catch (Throwable checked) {
            timer.setFailed();
            timer.addFieldToReport("checkedException", true);
            throw checked;
        } finally {
            timer.stop().report();
        }
    }

    public static Object timeMetode(Metodekall metodekall, String timerNavn) throws Throwable {
        return timeMetode(metodekall, timerNavn, MetricsFactory::createTimer);
    }
}
