package no.nav.brukerdialog.tools;

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

    public static String cookieDomain(UriInfo uri) {
        return cookieDomain(uri.getBaseUri().getHost());
    }

    public static String cookieDomain(HttpServletRequest request) {
        return cookieDomain(request.getServerName());
    }

    private static String cookieDomain(String serverName) {
        if (Boolean.valueOf(System.getProperty("develop-local", "false"))) {
            return null; //må ha null som domain når kjører lokalt
        }
        return serverName.substring(serverName.indexOf('.') + 1);
    }
}
