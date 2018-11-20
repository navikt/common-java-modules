package no.nav.metrics.aspects;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;

import static no.nav.metrics.proxy.MetricProxy.DO_NOT_MEASURE_METHOD_NAMES;

/**
 * @deprecated don't use aspects for metrics, just measure directly using MetricsFactory.getMeterRegistry()
 */
@Deprecated
public class AspectUtil {

    public static String lagMetodeTimernavn(JoinPoint joinPoint, String sattNavn) {
        if (StringUtils.isBlank(sattNavn)) {
            return getKlassenavn(joinPoint) + "." + getMetodenavn(joinPoint);
        } else {
            return sattNavn;
        }
    }

    public static String lagKlasseTimernavn(JoinPoint joinPoint, String sattNavn) {
        if (StringUtils.isBlank(sattNavn)) {
            return getKlassenavn(joinPoint) + "." + getMetodenavn(joinPoint);
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
