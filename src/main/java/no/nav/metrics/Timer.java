package no.nav.metrics;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Timer {
    /*
        Bruker både measureTimestamp og startTime fordi System.nanoTime()
        skal brukes for tidsmåling og System.currentTimeMillis() for å
        rapportere når målingen ble gjort.
     */
    private final MetricsClient metricsClient;
    private final String name;
    private long measureTimestamp;
    private long startTime;
    private long stopTime;
    private Map<String, Object> fields = new HashMap<>();

    Timer(MetricsClient metricsClient, String name) {
        this.metricsClient = metricsClient;
        this.name = name + ".timer";
    }

    public void start() {
        measureTimestamp = System.currentTimeMillis();
        startTime = System.nanoTime();
    }

    public void stop() {
        stopTime = System.nanoTime();

        addFieldToReport("value", getElpasedTimeInMillis());
    }

    private long getElpasedTimeInMillis() {
        long elapsedTimeNanos = stopTime - startTime;

        return NANOSECONDS.toMillis(elapsedTimeNanos);
    }

    public void addFieldToReport(String fieldName, Object value) {
        fields.put(fieldName, value);
    }

    public void report() {
        ensureTimerIsStopped();
        metricsClient.report(name, fields, measureTimestamp);
        reset();
    }

    private void ensureTimerIsStopped() {
        if (!fields.containsKey("value")) {
            throw new IllegalStateException("Must stop timer before reporting!");
        }
    }

    /**
     * Timer er ikke threadsafe, bruk en ny timer heller enn å resette en eksisterende
     * om flere tråder kan aksessere målepunktet samtidig
     */
    private void reset() {
        measureTimestamp = 0;
        startTime = 0;
        stopTime = 0;
        fields = new HashMap<>();
    }
}
