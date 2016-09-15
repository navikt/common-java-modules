package no.nav.metrics.aspects;

import org.aspectj.lang.JoinPoint;

public class AspectUtil {

    public static String lagMetodeTimernavn(JoinPoint joinPoint, String sattNavn) {
        if (sattNavn.equals("")) {
            return  getKlassenavn(joinPoint) + "." + getMetodenavn(joinPoint);
        } else {
            return sattNavn;
        }
    }


    public static String getKlassenavn(JoinPoint joinPoint) {
//        return joinPoint.getTarget().getClass().getSimpleName(); TODO denne?
        return joinPoint.getSignature().getDeclaringType().getSimpleName();
    }


    public static String getMetodenavn(JoinPoint joinPoint) {
        return joinPoint.getSignature().getName();
    }

}
