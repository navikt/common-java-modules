package no.nav.metrics.aspects;

import no.nav.metrics.MetodeEvent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static no.nav.metrics.aspects.AspectUtil.*;

/**
 * @deprecated don't use aspects for metrics, just measure directly using MetricsFactory.getMeterRegistry()
 */
@Deprecated
@Aspect
@Component
public class CountAspect {

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @Around("publicMethod() && @annotation(count)")
    public Object countPaMetode(ProceedingJoinPoint joinPoint, Count count) throws Throwable {
        AspectMetodekall metodekall = new AspectMetodekall(joinPoint);
        String eventNavn = lagMetodeTimernavn(joinPoint, count.name());

        return MetodeEvent.eventForMetode(metodekall, eventNavn, finnArgumentVerdier(joinPoint, count));
    }

    @Around("publicMethod() && @within(count)")
    public Object countPaKlasse(ProceedingJoinPoint joinPoint, Count count) throws Throwable {
        if (metodeSkalIgnoreres(getMetodenavn(joinPoint), count.ignoredMethods())) {
            return joinPoint.proceed();
        }

        AspectMetodekall metodekall = new AspectMetodekall(joinPoint);
        String eventNavn = lagKlasseTimernavn(joinPoint, count.name());

        return MetodeEvent.eventForMetode(metodekall, eventNavn);
    }

    private Map<String, String> finnArgumentVerdier(JoinPoint joinPoint, Count count) {
        if (count.fields().length == 0) {
            return null;
        }

        Map<String, String> verdier = new HashMap<>();

        Object[] args = joinPoint.getArgs();

        for (Field field : count.fields()) {
            String value = args[parseInt(field.argumentNumber()) - 1].toString();
            verdier.put(field.key(), value);
        }

        return verdier;
    }
}
