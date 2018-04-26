package no.nav.modig.security.util;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Laster properties-filer fra classpath.
 * <p/>
 * Har convenience-metoder for å laste properties med default verdi.
 */
public class PropertySource {
    private static final Logger logger = LoggerFactory.getLogger(PropertySource.class);

    private Properties properties;

    public PropertySource(String fileName) {
        Properties props = new Properties();

        URL url = ClassLoaderUtils.getResource(fileName, this.getClass());
        if (url != null) {
            try {
                InputStream in = url.openStream();
                props.load(in);
                in.close();
            } catch (IOException e) {
                logger.info("Property file \"" + fileName + "\" error: " + e.getMessage());
            }
        } else {
            logger.info("Property file \"" + fileName + "\" not found on classpath");
        }
        properties = props;
    }

    public PropertySource(Properties properties) {
        this.properties = properties;
    }

    public final String getProperty(String propKey, String defaultValue) {
        return properties.getProperty(propKey, defaultValue);
    }

    public final String getProperty(String propKey) {
        String prop = (String) properties.get(propKey);
        if (prop == null) {
            throw new RuntimeException("Property \"" + propKey + "\" not found");
        }
        return prop;
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }
}
