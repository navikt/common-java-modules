package no.nav.common.feed.util;

import lombok.SneakyThrows;
import no.nav.common.metrics.MetodeTimer;

import java.util.function.Supplier;

import static no.nav.common.metrics.MetricsFactory.createEvent;

public class MetricsUtils {

    @SneakyThrows
    public static <S> S timed(String name, Supplier<S> supplier) {
        return (S) MetodeTimer.timeMetode(supplier::get, name);
    }

    public static void metricEvent(String eventName, String feedName) {
        createEvent("feed." + eventName)
                .addTagToReport("feedname", feedName)
                .report();
    }

}
