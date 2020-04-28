package no.nav.common.utils;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static no.nav.common.utils.EnvironmentUtils.EnviromentClass.UNKNOWN;
import static no.nav.common.utils.StringUtils.nullOrEmpty;
import static no.nav.common.utils.StringUtils.of;

public class EnvironmentUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUtils.class);

    public static final String APP_NAME_PROPERTY_NAME = "NAIS_APP_NAME";
    public static final String NAIS_NAMESPACE_PROPERTY_NAME = "NAIS_NAMESPACE";
    public static final String NAIS_CLUSTER_NAME_PROPERTY_NAME = "NAIS_CLUSTER_NAME";

    public static final String FASIT_ENVIRONMENT_NAME_PROPERTY_NAME = "FASIT_ENVIRONMENT_NAME";
    public static final String APP_ENVIRONMENT_NAME_PROPERTY_NAME = "APP_ENVIRONMENT_NAME";


    public static final String APP_VERSION_PROPERTY_NAME = "APP_VERSION";

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

    public static boolean getPropertyAsBooleanOrElseFalse(String propertyName) {
        return EnvironmentUtils
                .getOptionalProperty(propertyName)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    public static EnviromentClass getEnvironmentClass() {
        return getEnvironmentName()
                .map(e -> Character.toString(e.charAt(0)).toUpperCase())
                .map(EnviromentClass::valueOf)
                .orElse(UNKNOWN);
    }

    public static boolean isEnvironmentClass(EnviromentClass enviromentClass) {
        return getEnvironmentClass().equals(enviromentClass);
    }

    public static Optional<String> getApplicationName() {
        return getOptionalProperty(APP_NAME_PROPERTY_NAME);
    }

    public static String requireApplicationName() {
        return getApplicationName().orElseThrow(() -> new IllegalStateException(createErrorMessage(APP_NAME_PROPERTY_NAME)));
    }

    public static Optional<String> getEnvironmentName() {
        return getOptionalProperty(APP_ENVIRONMENT_NAME_PROPERTY_NAME, FASIT_ENVIRONMENT_NAME_PROPERTY_NAME);
    }

    public static String requireEnvironmentName() {
        return getEnvironmentName().orElseThrow(() -> new IllegalStateException(createErrorMessage(APP_ENVIRONMENT_NAME_PROPERTY_NAME, FASIT_ENVIRONMENT_NAME_PROPERTY_NAME)));
    }

    public static Optional<String> getApplicationVersion() {
        return getOptionalProperty(APP_VERSION_PROPERTY_NAME);
    }

    public static Optional<String> getNamespace() {
        return getOptionalProperty(NAIS_NAMESPACE_PROPERTY_NAME);
    }

    public static Optional<String> getClusterName() {
        return getOptionalProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME);
    }

    public static String requireNamespace() {
        return getNamespace().orElseThrow(() -> new IllegalStateException(createErrorMessage(NAIS_NAMESPACE_PROPERTY_NAME)));
    }

    @SneakyThrows
    public static String resolveHostName() {
        return InetAddress.getLocalHost().getCanonicalHostName();
    }

    private static String createErrorMessage(String propertyName, String... otherPropertyNames) {
        if (otherPropertyNames == null) {
            return "mangler property: " + propertyName;
        } else {
            String properties = Stream.concat(Stream.of(propertyName), Stream.of(otherPropertyNames)).collect(joining(", "));
            return "fant ingen av propertyene: " + properties;
        }
    }

    private static String getProperty(String propertyName) {
        return System.getProperty(propertyName, System.getenv(propertyName));
    }

    public static String resolveSrvUserPropertyName() {
        return asNaisProperty("SRV" + resolveApplicationName() + "_USERNAME");
    }

    public static String resolverSrvPasswordPropertyName() {
        return asNaisProperty("SRV" + resolveApplicationName() + "_PASSWORD");
    }

    private static String resolveApplicationName() {
        return EnvironmentUtils.requireApplicationName().toUpperCase();
    }

    private static String asNaisProperty(String value) {
        return value.replaceAll("\\W", "_");
    }

    public enum EnviromentClass {
        UNKNOWN,
        T,
        Q,
        P,
        M // mock
    }

    public enum Type {
        SECRET,
        PUBLIC;

        public String format(String value) {
            if (this == Type.PUBLIC) {
                return value;
            }
            return "*******";
        }
    }


}