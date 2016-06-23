package no.nav.metrics.aspects;

import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TimerAspect {
    @Pointcut("@within(no.nav.metrics.aspects.Timed) || @annotation(no.nav.metrics.aspects.Timed)")
    public void timed() {
    }

    @SuppressWarnings("ProhibitedExceptionThrown")
    @Around(value = "timed()")
    public Object timer(ProceedingJoinPoint joinPoint) throws Throwable {
        String simpleName = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String method = joinPoint.getSignature().getName();
        Timer timer = createTimerForMethod(simpleName, method);
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

    public Timer createTimerForMethod(String simpleName, String method) {
        return MetricsFactory.createTimer(simpleName + "." + method);
    }
}
