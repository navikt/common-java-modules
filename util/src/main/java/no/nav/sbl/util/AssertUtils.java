package no.nav.sbl.util;

public class AssertUtils {

    public static <T> T assertNotNull(T object) {
        return assertNotNull(object, null);
    }

    public static <T> T assertNotNull(T object, String errorMessage) {
        if (object == null) {
            throw new IllegalStateException(errorMessage);
        }
        return object;
    }

    public static boolean assertTrue(boolean bool) {
        if (!bool) {
            throw new IllegalStateException();
        }
        return bool;
    }
}
