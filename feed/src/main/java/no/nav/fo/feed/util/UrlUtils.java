package no.nav.fo.feed.util;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class UrlUtils {

    public final static String QUERY_PARAM_PAGE_SIZE = "page_size";
    public final static String QUERY_PARAM_ID = "id";

    private static Pattern pattern = Pattern.compile("([^:]\\/)\\/+");

    public static String callbackUrl(String root, String feedname) {
        String environmentClass = System.getProperty("environment.class");
        String environment = System.getProperty("environment.name");
        String host = getHost(environmentClass, environment);

        return asUrl(host, root, "feed", feedname);
    }

    static String getHost(String environmentClass, String environment) {
        switch (environmentClass) {
            case "lokalt":
                return System.getProperty("feed.lokalt.callback.host");
            case "u":
            case "t":
            case "q": {
                return format("https://app-%s.adeo.no", environment);
            }
            default: {
                return "https://app.adeo.no";
            }
        }
    }

    public static String asUrl(String... s) {
        String url = Stream.of(s).collect(Collectors.joining("/"));
        return pattern.matcher(url).replaceAll("$1");
    }
}
