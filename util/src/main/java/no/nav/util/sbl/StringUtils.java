package no.nav.util.sbl;

import java.util.Objects;
import java.util.Optional;

public class StringUtils {

    public static boolean notNullOrEmpty(String string) {
        return string != null && string.trim().length() > 0;
    }

    public static boolean isEqualIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return Objects.equals(a, b);
        }

        return a.toLowerCase().equals(b.toLowerCase());
    }

    public static Optional<String> of(String string) {
        return Optional.ofNullable(string).filter(StringUtils::notNullOrEmpty);
    }

    public static boolean nullOrEmpty(String string) {
        return !notNullOrEmpty(string);
    }

    public static String assertNotNullOrEmpty(String string) {
        if (nullOrEmpty(string)) {
            throw new IllegalStateException();
        }
        return string;
    }

    public static String toString(Object o) {
        return o != null ? o.toString() : "";
    }

    public static String substring(String string, int fromIndex) {
        if (string == null) {
            return "";
        }
        return string.substring(Math.max(0, Math.min(fromIndex, string.length())));
    }

    public static String substring(String string, int fromIndex, int toIndex) {
        if (string == null) {
            return "";
        }
        int length = string.length();
        int fromIndexBounded = Math.max(0, Math.min(fromIndex, length));
        return string.substring(fromIndexBounded, Math.max(fromIndexBounded, Math.min(toIndex, length)));
    }

}
