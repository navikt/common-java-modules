package no.nav.apiapp.util;

import java.util.Optional;
import java.util.Properties;

import static java.util.Optional.ofNullable;
import static no.nav.apiapp.util.StringUtils.of;

public class PropertyUtils {

    public static String getRequiredProperty(String propertyName) {
        return of(System.getProperty(propertyName))
                .orElseThrow(() -> new IllegalStateException("mangler property: " + propertyName));
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getOptionalObjectProperty(Class<T> forClass) {
        Properties properties = System.getProperties();
        return ofNullable(
                ofNullable((T) properties.get(forClass)).orElseGet(() -> (T) properties.get(forClass.getName()))
        );
    }

}
