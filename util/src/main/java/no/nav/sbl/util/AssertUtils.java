package no.nav.sbl.util;

public class AssertUtils {

    public static <T> T assertNotNull(T object) {
        if (object == null) {
            throw new IllegalStateException();
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
