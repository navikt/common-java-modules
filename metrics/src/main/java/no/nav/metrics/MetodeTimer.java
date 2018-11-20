package no.nav.metrics;

import lombok.SneakyThrows;

public class MetodeTimer {

    @SneakyThrows
    public static Object timeMetode(Metodekall metodekall, String timerNavn) {
        Timer timer = MetricsFactory.createTimer(timerNavn);

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
}
