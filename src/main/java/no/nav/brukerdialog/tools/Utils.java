package no.nav.brukerdialog.tools;


import no.nav.brukerdialog.security.oidc.OidcTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    public static String getSystemProperty(String propertyName) {
        String property = System.getProperty(propertyName);
        if (property == null || property.trim().length() == 0) {
            throw new OidcTokenException("Mangler system property: " + propertyName);
        }
        return property;
    }

    public static String getRelativePath(String path) {
        if (path == null) {
            return path;
        } else if (!path.contains("http")) {
            return path;
        }
        return path.replaceAll(".*:\\/\\/[^\\/]*\\/", "/");
    }
}
