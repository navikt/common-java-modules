package no.nav.metrics.aspects;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Count {
    String name() default "";
    Field[] fields() default {};
}

