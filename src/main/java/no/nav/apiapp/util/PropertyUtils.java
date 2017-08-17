package no.nav.apiapp.util;

import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.apiapp.util.StringUtils.of;

public class PropertyUtils {

    public static String getRequiredProperty(String propertyName) {
        return of(System.getProperty(propertyName))
                .orElseThrow(() -> new IllegalStateException("mangler property: " + propertyName));
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getOptionalObjectProperty(Class<T> forClass) {
        return ofNullable((T) System.getProperties().get(forClass.getName()));
    }

}
