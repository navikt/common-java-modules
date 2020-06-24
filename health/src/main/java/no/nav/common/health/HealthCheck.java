package no.nav.common.health;

public interface HealthCheck {

    HealthCheckResult checkHealth();

    default HealthCheckMetadata getMetadata() {
        return new HealthCheckMetadata("Helsesjekk");
    };

}
