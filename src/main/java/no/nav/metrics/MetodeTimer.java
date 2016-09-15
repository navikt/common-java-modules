package no.nav.metrics;

public class MetodeTimer {

    public static Object timeMetode(Metodekall metodekall, String timerNavn) throws Throwable {
        Timer timer = MetricsFactory.createTimer(timerNavn);

        try {
            timer.start();
            Object resultat = metodekall.kallMetode();
            timer.addFieldToReport("success", true);
            return resultat;
        } catch (Throwable throwable) {
            timer.addFieldToReport("success", false);
            throw throwable;
        } finally {
            timer.stop();
            timer.report();
        }

    }
}
