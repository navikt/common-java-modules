package no.nav.metrics.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Integer.*;
import static java.util.Arrays.*;
import static java.util.Collections.emptyList;
import static no.nav.metrics.MetricsFactory.createEvent;
import static no.nav.metrics.aspects.AspectUtil.getAspectName;

/**
 * HOWTO:
 * - aspectjweaver som en runtime dependency
 * - @EnableAspectJAutoProxy i Spring-config
 * - Gjør CountAspect tilgjengelig som en Spring-bean
 * - Sørg for at klassen der du bruker @Count er managed av Spring
 *
 * Kan brukes f. eks. slik:
 *
 *
 @Count
 public void methodname() {

 }

 eller slik:

 @Count(name = "eventnavn")
 public void methodname() {

 }


 eller om du vil ha med et field i rapporten, slik:

 @Count(name = "eventnavn", fields = {@Field(key = "orgnummer", argumentNumber = "2")})
 public void methodname(String id, String orgnummer) {
 }

 *
 */
@Aspect
@Component
public class CountAspect {

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @SuppressWarnings("ProhibitedExceptionThrown")
    @Around("publicMethod() && @annotation(count)")
    public Object count(ProceedingJoinPoint joinPoint, Count count) throws Throwable {
        createEvent(getAspectName(joinPoint, count.name())).withFields(joinPoint, count).report();
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw e;
        }
    }
}
