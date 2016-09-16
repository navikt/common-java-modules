package no.nav.metrics.aspects;

import org.aspectj.lang.JoinPoint;

import static no.nav.metrics.proxy.MetricProxy.DO_NOT_MEASURE_METHOD_NAMES;


public class AspectUtil {

    public static String lagMetodeTimernavn(JoinPoint joinPoint, String sattNavn) {
        if (sattNavn.equals("")) {
            return  getKlassenavn(joinPoint) + "." + getMetodenavn(joinPoint);
        } else {
            return sattNavn;
        }
    }

    public static String lagKlasseTimernavn(JoinPoint joinPoint, String sattNavn) {
        if (sattNavn.equals("")) {
            return  getKlassenavn(joinPoint) + "." + getMetodenavn(joinPoint);
        } else {
            return sattNavn + "." + getMetodenavn(joinPoint);
        }
    }

    public static String getKlassenavn(JoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringType().getSimpleName();
    }

    public static String getMetodenavn(JoinPoint joinPoint) {
        return joinPoint.getSignature().getName();
    }

    public static boolean metodeSkalIgnoreres(String metodeNavn, String[] ignorerteMetoder) {
        for (String ignorert : ignorerteMetoder) {
            if (metodeNavn.equals(ignorert)) {
                return true;
            }
        }

        return DO_NOT_MEASURE_METHOD_NAMES.contains(metodeNavn);
    }
}
