package no.nav.apiapp.util;

public class ObjectUtils {

    public static boolean notEqual(Object a, Object b) {
        return !isEqual(a, b);
    }

    public static boolean isEqual(Object a, Object b) {
        if (a != null && b != null) {
            return a.equals(b);
        } else {
            return a == b; // NOSONAR
        }
    }

    public static <T extends Comparable<T>> T min(T a, T b) {
        return a != null && (b == null || a.compareTo(b) < 0) ? a : b;
    }

    public static <T extends Comparable<T>> T max(T a, T b) {
        return a != null && (b == null || a.compareTo(b) > 0) ? a : b;
    }

}