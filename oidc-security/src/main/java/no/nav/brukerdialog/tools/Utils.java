package no.nav.brukerdialog.tools;


import no.nav.brukerdialog.security.oidc.OidcTokenException;

import java.util.List;

import static java.util.Arrays.asList;

public class Utils {

    public static String getSystemProperty(String propertyName) {
        String property = System.getProperty(propertyName);
        if (property == null || property.trim().length() == 0) {
            throw new OidcTokenException("Mangler system property: " + propertyName);
        }
        return property;
    }

    public static List<String> getCommaSeparatedUsers(String users) {
        return asList(users.trim().toLowerCase().split(","));
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
