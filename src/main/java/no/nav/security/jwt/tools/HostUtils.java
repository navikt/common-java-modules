package no.nav.security.jwt.tools;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

public class HostUtils {

    public static String formatSchemeHostPort(UriInfo uriInfo) {
        URI baseUri = uriInfo.getBaseUri();
        return HostUtils.formatSchemeHostPort(baseUri.getScheme(), baseUri.getHost(), baseUri.getPort());
    }

    public static String formatSchemeHostPort(HttpServletRequest req) {
        return formatSchemeHostPort(req.getScheme(), req.getServerName(), req.getServerPort());
    }

    public static String formatSchemeHostPort(String scheme, String name, int port) {
        return port < 0 || port == 443 && "https".equals(scheme) || port == 80 && "http".equals(scheme)
                ? formatSchemeHost(scheme, name)
                : formatSchemeHost(scheme, name) + ":" + port;
    }

    public static String formatSchemeHost(String scheme, String name) {
        return scheme + "://" + name;
    }
}
