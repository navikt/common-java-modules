package no.nav.sbl.dialogarena.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.TreeSet;

import static no.nav.modig.lang.collections.IterUtils.on;


public final class SystemProperties {

    private static final Logger LOG = LoggerFactory.getLogger(SystemProperties.class);

    public static void setFrom(String resource) {
        InputStream propsResource = SystemProperties.class.getClassLoader().getResourceAsStream(resource);
        if (propsResource == null) {
            throw new NotFound(resource);
        }
        setFrom(propsResource);
    }

    public static void setFrom(InputStream input) {
        Properties props = new Properties();
        try {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lese properties", e);
        }
        Properties target = System.getProperties();
        for (String name : on(props.stringPropertyNames()).collectIn(new TreeSet<String>())) {
            String value = props.getProperty(name);
            if (target.containsKey(name)) {
                LOG.warn("Old value '{}' is replaced with", target.getProperty(name));
                LOG.warn("{} = {}", name, value);
            } else {
                LOG.info("Setting {} = '{}'", name, value);
            }
            target.setProperty(name, value);
        }
    }

    public static final class NotFound extends RuntimeException {
        private NotFound(String resourceName) {
            super(resourceName + " not found");
        }
    }

    private SystemProperties() { }

}
