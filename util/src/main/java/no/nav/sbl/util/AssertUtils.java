package no.nav.sbl.util;

public class AssertUtils {

    public static void assertNotNull(Object object) {
        if (object == null) {
            throw new IllegalStateException();
        }
    }

}
