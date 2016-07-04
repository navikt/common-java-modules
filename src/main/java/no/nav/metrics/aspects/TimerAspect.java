package no.nav.metrics.aspects;

import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
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
    public Object timer(final ProceedingJoinPoint joinPoint, final Timed timed) throws Throwable {
        final String signature;
        if (timed.name().equals("")) {
            final String simpleName = joinPoint.getSignature().getDeclaringType().getSimpleName();
            final String method = joinPoint.getSignature().getName();
            signature = simpleName + "." + method;
        } else {
            signature = timed.name();
        }

        final Timer timer = createTimerForMethod(signature);
        timer.start();
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            timer.addFieldToReport("feilet", e.getMessage());
            throw e;
        } finally {
            timer.stop();
            timer.report();
        }
    }

    private Timer createTimerForMethod(String signature) {
        return MetricsFactory.createTimer(signature);
    }
}
