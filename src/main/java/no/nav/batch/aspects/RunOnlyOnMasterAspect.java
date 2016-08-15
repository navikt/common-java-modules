package no.nav.batch.aspects;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import static java.lang.System.getProperty;

/**
 * HOWTO:
 * - aspectjweaver som en runtime dependency
 * - @EnableAspectJAutoProxy i Spring-config
 * - Gj�r RunOnlyOnMasterAspect tilgjengelig som en Spring-bean
 * - S�rg for at klassen der du bruker @RunOnlyOnMaster er managed av Spring
 */
@Aspect
@Component
public class RunOnlyOnMasterAspect {

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @Around("publicMethod() && @annotation(runOnlyOnMaster)")
    public Object runOnlyOnMaster(ProceedingJoinPoint joinPoint, RunOnlyOnMaster runOnlyOnMaster) {
        if ("true".equals(getProperty("cluster.ismasternode"))) {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
