package no.nav.common.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.IOUtils.closeQuietly;


public final class SystemProperties {

    private static final Logger LOG = LoggerFactory.getLogger(SystemProperties.class);

    public static void setTemporaryProperty(String name, String value, Runnable runnable) {
        String originalProperty = System.getProperty(name);
        try {
            if (value != null) {
                System.setProperty(name, value);
            } else {
                System.clearProperty(name);
            }

            LOG.info("{} = {}", name, value);
            runnable.run();
        } finally {
            if (originalProperty != null) {
                System.setProperty(name, originalProperty);
            } else {
                System.clearProperty(name);
            }
        }
    }

    public static void setFrom(String resource) {
        InputStream propsResource = SystemProperties.class.getClassLoader().getResourceAsStream(resource);
        if (propsResource == null) {
            throw new NotFound(resource);
        }
        setFrom(propsResource);
        closeQuietly(propsResource);
    }

    public static void setFrom(InputStream input) {
        Properties props = new Properties();
        try {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lese properties", e);
        }
        Properties target = System.getProperties();
        for (String name : props.stringPropertyNames().stream().collect(toSet())) {
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

    private SystemProperties() {
    }

}
