package no.nav.sbl.leaderelection.aspects;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RunOnlyOnLeader {
}

