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

}