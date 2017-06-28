package no.nav.brukerdialog.tools;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    public static String getSystemProperty(String propertyName) {
        String property = System.getProperty(propertyName);
        if(property == null) {
            log.error("System property " + propertyName + " er ikke definert");
            return null;
        }
        return property;
    }

    public static String getRelativePath(String path) {
        if(!path.contains("http")) {
            return path;
        } else if(!path.contains(".no")) {
            return path.split(":\\d{4}")[1];
        }
        return path.split("\\.no")[1];
    }
}
