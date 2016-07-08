package no.nav.metrics.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import static no.nav.metrics.MetricsFactory.createEvent;
import static no.nav.metrics.aspects.AspectUtil.getAspectName;

/**
 * HOWTO:
 * - aspectjweaver som en runtime dependency
 * - @EnableAspectJAutoProxy i Spring-config
 * - Gjør CountAspect tilgjengelig som en Spring-bean
 * - Sørg for at klassen der du bruker @Timed er managed av Spring
 */
@Aspect
@Component
public class CountAspect {

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @SuppressWarnings("ProhibitedExceptionThrown")
    @Around("publicMethod() && @annotation(count)")
    public Object count(final ProceedingJoinPoint joinPoint, final Count count) throws Throwable {
        createEvent(getAspectName(joinPoint, count.name())).withFields(count.fields()).report();
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw e;
        }
    }
}
