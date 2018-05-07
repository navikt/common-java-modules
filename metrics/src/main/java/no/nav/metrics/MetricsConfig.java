package no.nav.metrics;

import lombok.Value;

import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;

@Value
public class MetricsConfig {

    public static final String SENSU_CLIENT_HOST = "sensu_client_host";
    public static final String SENSU_CLIENT_PORT = "sensu_client_port";

    private String host;
    private int port;

    public static MetricsConfig resoleSkyaConfig() {
        return defaulted("localhost", 3030);
    }

    public static MetricsConfig resolveNaisConfig() {
        return defaulted("sensu.nais", 3030);
    }

    private static MetricsConfig defaulted(String localhost, int other) {
        return new MetricsConfig(
                getOptionalProperty(SENSU_CLIENT_HOST).orElse(localhost),
                getOptionalProperty(SENSU_CLIENT_PORT).map(Integer::parseInt).orElse(other)
        );
    }


}
