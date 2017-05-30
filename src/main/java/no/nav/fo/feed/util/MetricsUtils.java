package no.nav.fo.feed.util;

import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;

import java.util.function.Supplier;

public class MetricsUtils {
    public static <S> S timed(String name, Supplier<S> supplier) {
        Timer timer = MetricsFactory.createTimer(name);
        timer.start();
        try {
            return supplier.get();
        } catch (Exception e) {
            timer.setFailed();
            throw e;
        } finally {
            timer.stop();
            timer.report();
        }
    }
}
