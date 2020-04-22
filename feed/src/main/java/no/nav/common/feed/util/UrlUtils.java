package no.nav.common.feed.util;

import no.nav.common.utils.EnvironmentUtils;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;

public class UrlUtils {

    public static final String FEED_LOKALT_CALLBACK_HOST_PROPERTY_NAME = "feed.lokalt.callback.host";

    public final static String QUERY_PARAM_PAGE_SIZE = "page_size";
    public final static String QUERY_PARAM_ID = "id";

    private static Pattern pattern = Pattern.compile("([^:]\\/)\\/+");

    public static String callbackUrl(String root, String feedname) {
        return asUrl(getHost(), root, "feed", feedname);
    }

    private static String getHost() {
        return getOptionalProperty(FEED_LOKALT_CALLBACK_HOST_PROPERTY_NAME).orElseGet(() -> {
            switch (EnvironmentUtils.getEnvironmentClass()) {
                case T:
                case Q:
                    return format("https://app-%s.adeo.no", EnvironmentUtils.requireEnvironmentName());
                default:
                    return "https://app.adeo.no";
            }
        });
    }

    public static String asUrl(String... s) {
        String url = Stream.of(s).collect(Collectors.joining("/"));
        return pattern.matcher(url).replaceAll("$1");
    }
}
