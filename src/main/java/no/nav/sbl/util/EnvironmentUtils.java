package no.nav.sbl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.UKNOWN;
import static no.nav.sbl.util.StringUtils.nullOrEmpty;
import static no.nav.sbl.util.StringUtils.of;

public class EnvironmentUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUtils.class);

    public static final String ENVIRONMENT_CLASS_PROPERTY_NAME = "environment.class";

    public static void setProperty(String name, String value, Type type) {
        LOGGER.info("{}={}", name, type.format(value));
        System.setProperty(name, value);
    }

    public static String getRequiredProperty(String propertyName, String... otherPropertyNames) {
        return getOptionalProperty(propertyName, otherPropertyNames)
                .orElseThrow(() -> new IllegalStateException(createErrorMessage(propertyName, otherPropertyNames)));
    }

    public static Optional<String> getOptionalProperty(String propertyName, String... otherPropertyNames) {
        String propertyValue = EnvironmentUtils.getProperty(propertyName);
        if (nullOrEmpty(propertyValue) && otherPropertyNames != null) {
            propertyValue = Arrays.stream(otherPropertyNames)
                    .map(EnvironmentUtils::getProperty)
                    .filter(StringUtils::notNullOrEmpty)
                    .findFirst()
                    .orElse(null);
        }

        return of(propertyValue);
    }

    public static EnviromentClass getEnvironmentClass() {
        return getOptionalProperty(ENVIRONMENT_CLASS_PROPERTY_NAME)
                .map(String::toUpperCase)
                .flatMap(environmentClass -> EnumUtils.valueOf(EnviromentClass.class, environmentClass))
                .orElse(UKNOWN);
    }

    public static boolean isEnvironmentClass(EnviromentClass enviromentClass) {
        return getEnvironmentClass().equals(enviromentClass);
    }

    private static String createErrorMessage(String propertyName, String[] otherPropertyNames) {
        if (otherPropertyNames == null) {
            return "mangler property: " + propertyName;
        } else {
            return "fant ingen av propertyene: " + propertyName + ", " + Stream.of(otherPropertyNames).collect(Collectors.joining(", "));
        }
    }

    private static String getProperty(String propertyName) {
        return System.getProperty(propertyName, System.getenv(propertyName));
    }

    public enum EnviromentClass {
        UKNOWN,
        T,
        Q,
        P
    }

    public enum Type {
        SECRET,
        PUBLIC;

        public String format(String value) {
            switch (this) {
                case PUBLIC:
                    return value;
            }
            return "*******";
        }
    }


}
