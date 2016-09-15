package no.nav.metrics;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class TestUtil {


    public static void resetMetrics() {
        try {
            // Lukk øynene
            Field metricsClient = MetricsFactory.class.getDeclaredField("metricsClient");
            metricsClient.setAccessible(true);

            // Fjerner final lol
            Field modifier = Field.class.getDeclaredField("modifiers");
            modifier.setAccessible(true);
            modifier.setInt(metricsClient, metricsClient.getModifiers() &~Modifier.FINAL);

            metricsClient.set(null, new MetricsClient());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }
}
