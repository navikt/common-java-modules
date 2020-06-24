package no.nav.common.health;

import no.nav.common.health.selftest.HealthCheckMetadata;

public interface HealthCheck {

    HealthCheckResult checkHealth();

    HealthCheckMetadata healthCheckMetadata();

}
