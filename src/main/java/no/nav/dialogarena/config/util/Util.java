package no.nav.dialogarena.config.util;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class Util {

    private static final Logger LOG = getLogger(Util.class);

    public static void setProperty(String propertyName, String value) {
        LOG.info("property {} = {}", propertyName, value);
        System.setProperty(propertyName, value);
    }

}
