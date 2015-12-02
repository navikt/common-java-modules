package no.nav.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    Timer(MetricsClient metricsClient, String name) {
        this.metricsClient = metricsClient;
        this.name = name;
    }

    public void start() {
        long currentTime = System.currentTimeMillis();
        measureTimestamp = TimeUnit.MILLISECONDS.toSeconds(currentTime);
        startTime = System.nanoTime();
    }

    public void stop() {
        stopTime = System.nanoTime();
        report();
    }

    public void report() {
        long elapsedTimeNanos = getElapsedTime();
        long elapsedTimeMillis = TimeUnit.NANOSECONDS.toMillis(elapsedTimeNanos);

        Map<String, Object> fields = new HashMap<>();
        fields.put("value", elapsedTimeMillis);

        metricsClient.report(name, fields, measureTimestamp);
    }

    private long getElapsedTime() {
        return stopTime - startTime;
    }
}
