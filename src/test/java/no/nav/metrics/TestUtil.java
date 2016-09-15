package no.nav.metrics;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class TestUtil {

    public static <T> T lagAspectProxy(T target, Object aspect) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

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
