package no.nav.common.metrics.aspects;

import no.nav.common.metrics.MetodeTimer;
import no.nav.common.metrics.Metodekall;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * HOWTO:
 * - aspectjweaver som en runtime dependency
 * - @EnableAspectJAutoProxy i Spring-config
 * - Gjør TimerAspect tilgjengelig som en Spring-bean
 * - Sørg for at klassen der du bruker @Timed er managed av Spring
 */
@Aspect
@Component
public class TimerAspect {
    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @SuppressWarnings("ProhibitedExceptionThrown")
    @Around("publicMethod() && @annotation(timed)")
    public Object timerPaMetode(final ProceedingJoinPoint joinPoint, final Timed timed) throws Throwable {
        AspectMetodekall metodekall = new AspectMetodekall(joinPoint);
        String timerName = AspectUtil.lagMetodeTimernavn(joinPoint, timed.name());

        return timeMetode(metodekall, timerName);
    }

    @SuppressWarnings("ProhibitedExceptionThrown")
    @Around("publicMethod() && @within(timed)")
    public Object timerPaKlasse(final ProceedingJoinPoint joinPoint, final Timed timed) throws Throwable {
        if (AspectUtil.metodeSkalIgnoreres(AspectUtil.getMetodenavn(joinPoint), timed.ignoredMethods())) {
            return joinPoint.proceed();
        }

        AspectMetodekall metodekall = new AspectMetodekall(joinPoint);
        String timerNavn = AspectUtil.lagKlasseTimernavn(joinPoint, timed.name());

        return timeMetode(metodekall, timerNavn);
    }

    Object timeMetode(Metodekall metodekall, String timerNavn) throws Throwable {
        return MetodeTimer.timeMetode(metodekall, timerNavn);
    }
}
