package no.nav.metrics.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

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

    @Before("publicMethod() && @annotation(count)")
    public void count(JoinPoint joinPoint, Count count) {
        createEvent(getAspectName(joinPoint, count.name())).withFields(joinPoint, count).report();
    }
}
