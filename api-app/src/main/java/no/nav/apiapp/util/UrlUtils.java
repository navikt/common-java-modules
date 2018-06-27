package no.nav.apiapp.util;

public class UrlUtils {

    public static String sluttMedSlash(String path) {
        if (path == null) {
            return "/";
        } else {
            return path.endsWith("/") ? path : path + "/";
        }
    }

    public static String startMedSlash(String path) {
        if (path == null) {
            return "/";
        } else {
            return path.startsWith("/") ? path : "/" + path;
        }
    }

}
