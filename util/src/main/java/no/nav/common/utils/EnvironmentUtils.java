package no.nav.common.utils;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static no.nav.common.utils.StringUtils.nullOrEmpty;
import static no.nav.common.utils.StringUtils.of;

public class EnvironmentUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUtils.class);

    public static final String NAIS_APP_NAME_PROPERTY_NAME = "NAIS_APP_NAME";
    public static final String NAIS_NAMESPACE_PROPERTY_NAME = "NAIS_NAMESPACE";
    public static final String NAIS_CLUSTER_NAME_PROPERTY_NAME = "NAIS_CLUSTER_NAME";

    public static final List<String> DEV_CLUSTERS = List.of("dev-fss", "dev-sbs", "dev-gcp");
    public static final List<String> PROD_CLUSTERS = List.of("prod-fss", "prod-sbs", "prod-gcp");

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

    public static Optional<Boolean> isProduction() {
        return getClusterName().map(PROD_CLUSTERS::contains);
    }

    public static Optional<Boolean> isDevelopment() {
        return getClusterName().map(DEV_CLUSTERS::contains);
    }

    public static Optional<String> getApplicationName() {
        return getOptionalProperty(NAIS_APP_NAME_PROPERTY_NAME);
    }

    public static Optional<String> getNamespace() {
        return getOptionalProperty(NAIS_NAMESPACE_PROPERTY_NAME);
    }

    public static Optional<String> getClusterName() {
        return getOptionalProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME);
    }


    public static String requireApplicationName() {
        return getApplicationName().orElseThrow(() -> new IllegalStateException(createErrorMessage(NAIS_APP_NAME_PROPERTY_NAME)));
    }

    public static String requireNamespace() {
        return getNamespace().orElseThrow(() -> new IllegalStateException(createErrorMessage(NAIS_NAMESPACE_PROPERTY_NAME)));
    }

    public static String requireClusterName() {
        return getClusterName().orElseThrow(() -> new IllegalStateException(createErrorMessage(NAIS_CLUSTER_NAME_PROPERTY_NAME)));
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
