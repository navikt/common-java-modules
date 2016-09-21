package no.nav.metrics;

public class MetodeTimer {

    public static Object timeMetode(Metodekall metodekall, String timerNavn) throws Throwable {
        Timer timer = MetricsFactory.createTimer(timerNavn);

        try {
            timer.start();
            return metodekall.kallMetode();
        } catch (Throwable throwable) {
            timer.setFailed();
            throw throwable;
        } finally {
            timer.stop();
            timer.report();
        }

    }
}
