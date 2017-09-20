package no.nav.sbl.util;

import java.util.Optional;

import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.UKNOWN;
import static no.nav.sbl.util.StringUtils.of;

public class EnvironmentUtils {

    public static final String ENVIRONMENT_CLASS_PROPERTY_NAME = "environment.class";

    public static String getRequiredProperty(String propertyName) {
        return getOptionalProperty(propertyName)
                .orElseThrow(() -> new IllegalStateException("mangler property: " + propertyName));
    }

    public static Optional<String> getOptionalProperty(String propertyName) {
        return of(System.getProperty(propertyName, System.getenv(propertyName)));
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

}
