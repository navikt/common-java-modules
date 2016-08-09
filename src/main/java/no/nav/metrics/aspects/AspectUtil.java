package no.nav.metrics.aspects;

import org.aspectj.lang.JoinPoint;

public class AspectUtil {

    public static String getAspectName(JoinPoint joinPoint, String name) {
        if (name.equals("")) {
            final String simpleName = joinPoint.getSignature().getDeclaringType().getSimpleName();
            final String method = joinPoint.getSignature().getName();
            return simpleName + "." + method;
        } else {
           return name;
        }
    }
}
