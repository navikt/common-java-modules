package no.nav.json;

import java.util.Optional;

class StringUtils {

    private static boolean notNullOrEmpty(String string) {
        return string != null && string.trim().length() > 0;
    }

    static Optional<String> of(String string) {
        return Optional.ofNullable(string).filter(StringUtils::notNullOrEmpty);
    }
}