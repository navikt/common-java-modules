package no.nav.sbl.util;

import java.util.Optional;

public class StringUtils {

    public static boolean notNullOrEmpty(String string) {
        return string != null && string.trim().length() > 0;
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

}