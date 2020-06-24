package no.nav.common.health.selftest;

import lombok.Value;

@Value
public class HealthCheckMetadata {

    String description;

    boolean isCritical;

    String endpoint;
}
