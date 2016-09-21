package no.nav.metrics.aspects;

import no.nav.metrics.Metodekall;
import org.aspectj.lang.ProceedingJoinPoint;

class AspectMetodekall implements Metodekall {

    private final ProceedingJoinPoint joinPoint;

    AspectMetodekall(ProceedingJoinPoint joinPoint) {
        this.joinPoint = joinPoint;
    }

    @Override
    public Object kallMetode() throws Throwable {
        return joinPoint.proceed();
    }
}
