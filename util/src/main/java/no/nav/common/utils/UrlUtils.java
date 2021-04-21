package no.nav.common.utils;

import lombok.NonNull;

public class UrlUtils {

    public static String createServiceUrl(@NonNull String appName) {
        return createServiceUrl(appName, false);
    }

    public static String createServiceUrl(@NonNull String appName, boolean withAppContextPath) {
        return createServiceUrl(appName, EnvironmentUtils.requireNamespace(), withAppContextPath);
    }

    public static String createServiceUrl(@NonNull String appName, @NonNull String namespace, boolean withAppContextPath) {
        String contextPath = withAppContextPath ? "/" + appName : "";
        return String.format("http://%s.%s.svc.nais.local%s", appName, namespace, contextPath);
    }

    public static String createInternalIngressUrl(@NonNull String appName) {
        return EnvironmentUtils.isDevelopment().orElse(false)
                ? createDevInternalIngressUrl(appName)
                : createProdInternalIngressUrl(appName);
    }

    public static String createProdInternalIngressUrl(@NonNull String appName) {
        return String.format("https://%s.intern.nav.no", appName);
    }

    public static String createDevInternalIngressUrl(@NonNull String appName) {
        return String.format("https://%s.dev.intern.nav.no", appName);
    }

    public static String createAppAdeoPreprodIngressUrl(@NonNull String appName, @NonNull String environment) {
        return String.format("https://app-%s.adeo.no/%s", environment, appName);
    }

    public static String createAppAdeoProdIngressUrl(@NonNull String appName) {
        return String.format("https://app.adeo.no/%s", appName);
    }

    public static String createNaisAdeoIngressUrl(@NonNull String appName, boolean withAppContextPath) {
        String contextPath = withAppContextPath ? "/" + appName : "";
        return String.format("https://%s.nais.adeo.no%s", appName, contextPath);
    }

    public static String createDevAdeoIngressUrl(@NonNull String appName, boolean withAppContextPath) {
        String contextPath = withAppContextPath ? "/" + appName : "";
        return String.format("https://%s.dev.adeo.no%s", appName, contextPath);
    }

    public static String createNaisPreprodIngressUrl(@NonNull String appName, @NonNull String environment, boolean withAppContextPath) {
        String contextPath = withAppContextPath ? "/" + appName : "";
        return String.format("https://%s-%s.nais.preprod.local%s", appName, environment, contextPath);
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
