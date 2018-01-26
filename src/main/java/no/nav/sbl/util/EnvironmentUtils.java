package no.nav.sbl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.UKNOWN;
import static no.nav.sbl.util.StringUtils.notNullOrEmpty;
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
        return getOptionalProperty(propertyName,otherPropertyNames)
                .orElseThrow(() -> new IllegalStateException("mangler property: " + propertyName));
    }

    public static Optional<String> getOptionalProperty(String propertyName, String... otherPropertyNames) {
        String propertyValue = System.getProperty(propertyName);
        if (nullOrEmpty(propertyValue) && otherPropertyNames != null) {
            propertyValue = Arrays.stream(otherPropertyNames)
                    .map(System::getProperty)
                    .filter(StringUtils::notNullOrEmpty)
                    .findFirst()
                    .orElse(null);
        }
        if (nullOrEmpty(propertyValue)) {
            propertyValue = System.getenv(propertyName);
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
            switch (this){
                case PUBLIC:
                    return value;
            }
            return "*******";
        }
    }


}
