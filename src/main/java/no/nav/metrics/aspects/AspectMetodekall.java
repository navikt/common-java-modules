package no.nav.metrics.aspects;

import no.nav.metrics.Metodekall;
import org.aspectj.lang.ProceedingJoinPoint;

public class AspectMetodekall implements Metodekall {


    private final ProceedingJoinPoint joinPoint;

    public AspectMetodekall(ProceedingJoinPoint joinPoint) {
        this.joinPoint = joinPoint;
    }

    @Override
    public Object kallMetode() throws Throwable {
        return joinPoint.proceed();
    }
}
