package no.nav.common.leaderelection.aspects;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RunOnlyOnLeader {
}

