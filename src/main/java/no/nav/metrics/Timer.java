package no.nav.metrics;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
        this.name = name;
    }

    public void start() {
        long currentTime = System.currentTimeMillis();
        measureTimestamp = MILLISECONDS.toSeconds(currentTime);
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
            throw new RuntimeException("Must stop timer before reporting!");
        }
    }

    private void reset() {
        measureTimestamp = startTime = stopTime = 0;
        fields = new HashMap<>();
    }
}
