package no.nav.metrics.aspects;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Field {
    String key();

    String argumentNumber() default "";
}
