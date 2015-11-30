package no.nav.metrics;

import java.util.concurrent.TimeUnit;

public class Timer {
    /*
        Bruker både measureTimestamp og startTime fordi System.nanoTime()
        skal brukes for tidsmåling og System.currentTimeMillis() for å
        rapportere når målingen ble gjort.
     */
    MetricsClient metricsClient;
    String name;
    long measureTimestamp;
    long startTime;
    long stopTime;

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
        long elapsedTimeNanos = getElapsedTime();
        long elapsedTimeMillis = TimeUnit.NANOSECONDS.toMillis(elapsedTimeNanos);

        metricsClient.report(name, Long.toString(elapsedTimeMillis), measureTimestamp);
    }

    private long getElapsedTime() {
        return stopTime - startTime;
    }
}
