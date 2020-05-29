package no.nav.common.utils;

public class MathUtils {

    // Precise linear interpolation method, which guarantees <returned value> = 'to' when t = 1.
    public static long linearInterpolation(long from, long to, float t) {
        t = (t > 1) ? 1 : Math.max(0, t);
        return (long) ((1 - t) * from + t * to);
    }

}
