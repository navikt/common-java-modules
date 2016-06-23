package no.nav.metrics.aspects;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timed {
    boolean methodAsName() default true;
    String name() default "";
}
