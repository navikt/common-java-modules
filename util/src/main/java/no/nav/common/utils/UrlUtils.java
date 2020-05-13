package no.nav.common.utils;

public class UrlUtils {

    public static String clusterUrlForApplication(String applicationName) {
        return clusterUrlForApplication(applicationName, false);
    }

    public static String clusterUrlForApplication(String applicationName, boolean withContextPath) {
        String clusterUrl = String.format("http://%s.%s.svc.nais.local",
                AssertUtils.assertNotNull(applicationName),
                EnvironmentUtils.requireNamespace()
        );

        return withContextPath ? clusterUrl + "/" + applicationName : clusterUrl;
    }

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

    public static String joinPaths(String... paths) {
        if (paths == null) {
            return "/";
        }

        boolean lastEndsWithSlash = false;
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            if (path == null || path.isEmpty() || "/".equals(path)) {
                continue;
            }
            sb.append(lastEndsWithSlash ? path.startsWith("/") ? path.substring(1) : path : path.contains("://") ? path : startMedSlash(path));
            lastEndsWithSlash = path.endsWith("/");
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

}
