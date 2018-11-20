package no.nav.metrics.aspects;

import java.lang.annotation.*;

/**
 * @deprecated don't use aspects for metrics, just measure directly using MetricsFactory.getMeterRegistry()
 */
@Deprecated
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Field {
    String key();

    String argumentNumber() default "";
}
