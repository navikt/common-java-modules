package no.nav.metrics;

public class MetricsFactory {
    public static MetricsClient metricsClient = new MetricsClient();

    public static Timer createTimer(String name) {
        return new Timer(metricsClient, name);
    }
}
