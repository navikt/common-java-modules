package no.nav.apiapp.util;

public class UrlUtils {

    public static String sluttMedSlash(String path) {
        return path == null || path.endsWith("/") ? path : path + "/";
    }

}
