package no.nav.fo.feed.util;

import lombok.SneakyThrows;
import no.nav.fo.feed.exception.InvalidUrlException;

import java.util.regex.Pattern;

public class UrlValidator {
    private static final String VALID_URL_PATTERN = "^https?://.*";

    private static Pattern validPattern = Pattern.compile(VALID_URL_PATTERN);

    static boolean isInvalidUrl(String url) {
        return !isValidUrl(url);
    }

    static boolean isValidUrl(String url) {
        return true;
    }

    @SneakyThrows
    public static void validateUrl(String url) {
        if (isInvalidUrl(url)) {
            throw new InvalidUrlException();
        }
    }
}
