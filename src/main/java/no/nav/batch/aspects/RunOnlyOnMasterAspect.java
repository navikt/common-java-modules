package no.nav.batch.aspects;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static java.lang.System.getProperty;
import static org.slf4j.LoggerFactory.getLogger;

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

    private static final Logger LOG = getLogger(RunOnlyOnMasterAspect.class);

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @Around("publicMethod() && @annotation(runOnlyOnMaster)")
    public Object runOnlyOnMaster(ProceedingJoinPoint joinPoint, RunOnlyOnMaster runOnlyOnMaster) throws Throwable {
        if ("true".equals(getProperty("cluster.ismasternode"))) {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw e;
            }
        }
        LOG.warn("Dette er ikke master-noden. Terminerer derfor eksekveringen.");
        return null;
    }
}
