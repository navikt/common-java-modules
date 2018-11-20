package no.nav.metrics.aspects;

import no.nav.metrics.MetodeTimer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import static no.nav.metrics.aspects.AspectUtil.*;

/**
 * @deprecated don't use aspects for metrics, just measure directly using MetricsFactory.getMeterRegistry()
 */
@Deprecated
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
        String timerName = lagMetodeTimernavn(joinPoint, timed.name());

        return MetodeTimer.timeMetode(metodekall, timerName);
    }

    @SuppressWarnings("ProhibitedExceptionThrown")
    @Around("publicMethod() && @within(timed)")
    public Object timerPaKlasse(final ProceedingJoinPoint joinPoint, final Timed timed) throws Throwable {
        if (metodeSkalIgnoreres(getMetodenavn(joinPoint), timed.ignoredMethods())) {
            return joinPoint.proceed();
        }

        AspectMetodekall metodekall = new AspectMetodekall(joinPoint);
        String timerNavn = lagKlasseTimernavn(joinPoint, timed.name());

        return MetodeTimer.timeMetode(metodekall, timerNavn);
    }
}
