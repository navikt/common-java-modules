package no.nav.metrics.aspects;

import no.nav.metrics.Event;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import static java.lang.Integer.parseInt;
import static no.nav.metrics.MetricsFactory.createEvent;
import static no.nav.metrics.aspects.AspectUtil.lagMetodeTimernavn;

/**
 * HOWTO:
 * - aspectjweaver som en runtime dependency
 * - @EnableAspectJAutoProxy i Spring-config
 * - Gjør CountAspect tilgjengelig som en Spring-bean
 * - Sørg for at klassen der du bruker @Count er managed av Spring
 *
 * Kan brukes f. eks. slik:
 * <pre>
 *
 * {@literal @}Count
 * public void methodname() {
 *
 * }
 *
 * eller slik:
 *
 * {@literal @}Count(name = "eventnavn")
 * public void methodname() {
 *
 * }
 *
 * eller om du vil ha med et field i rapporten, slik:
 *
 * {@literal @}Count(name = "eventnavn", fields = {{@literal @}Field(key = "orgnummer", argumentNumber = "2")})
 * public void methodname(String id, String orgnummer) {
 * }
 * </pre>
 *
 */
@Aspect
@Component
public class CountAspect {

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @Around("publicMethod() && @annotation(count)")
    public Object count(ProceedingJoinPoint joinPoint, Count count) throws Throwable {
        Event event = createEvent(lagMetodeTimernavn(joinPoint, count.name()));
        leggTilFelterPaEvent(event, joinPoint, count);
        event.report();

        return joinPoint.proceed();
    }


    private void leggTilFelterPaEvent(Event event, JoinPoint joinPoint, Count count) {
        Object[] args = joinPoint.getArgs();

        for (Field field : count.fields()) {
            String value = args[parseInt(field.argumentNumber()) - 1].toString();
            event.addFieldToReport(field.key(), value);
        }
    }
}
