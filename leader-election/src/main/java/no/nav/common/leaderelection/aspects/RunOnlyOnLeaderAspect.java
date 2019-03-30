package no.nav.common.leaderelection.aspects;


import no.nav.common.leaderelection.LeaderElection;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * HOWTO:
 * - aspectjweaver som en runtime dependency
 * - @EnableAspectJAutoProxy i Spring-config
 * - Gjør RunOnlyOnLeaderAspect tilgjengelig som en Spring-bean
 * - Sørg for at klassen der du bruker @RunOnlyOnLeader er managed av Spring
 */
@Aspect
@Component
public class RunOnlyOnLeaderAspect {

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @Around("publicMethod() && @annotation(runOnlyOnLeader)")
    public Object runOnlyOnLeader(ProceedingJoinPoint joinPoint, RunOnlyOnLeader runOnlyOnLeader) throws Throwable {
        if (LeaderElection.isLeader()) {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw e;
            }
        }
        return null;
    }
}
