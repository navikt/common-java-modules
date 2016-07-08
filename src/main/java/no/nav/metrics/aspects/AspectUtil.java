package no.nav.metrics.aspects;

import org.aspectj.lang.ProceedingJoinPoint;

public class AspectUtil {

    public static String getAspectName(ProceedingJoinPoint joinPoint, String name) {
        if (name.equals("")) {
            final String simpleName = joinPoint.getSignature().getDeclaringType().getSimpleName();
            final String method = joinPoint.getSignature().getName();
            return simpleName + "." + method;
        } else {
           return name;
        }
    }
}
