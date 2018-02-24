package no.nav.sbl.leaderelection.aspects;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import static no.nav.sbl.leaderelection.LeaderElection.isLeader;

/**
 * HOWTO:
 * - aspectjweaver som en runtime dependency
 * - @EnableAspectJAutoProxy i Spring-config
 * - Gj�r RunOnlyOnLeaderAspect tilgjengelig som en Spring-bean
 * - S�rg for at klassen der du bruker @RunOnlyOnLeader er managed av Spring
 */
@Aspect
@Component
public class RunOnlyOnLeaderAspect {

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @Around("publicMethod() && @annotation(runOnlyOnLeader)")
    public Object runOnlyOnLeader(ProceedingJoinPoint joinPoint, RunOnlyOnLeader runOnlyOnLeader) throws Throwable {
        if (isLeader()) {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw e;
            }
        }
        return null;
    }
}
