package no.nav.common.health;

import java.util.Optional;

public class HealthCheckResult {

    private final boolean isHealthy;

    private final String errorMessage;

    private final Throwable error;

    private HealthCheckResult(boolean isHealthy, String errorMessage, Throwable error) {
        this.isHealthy = isHealthy;
        this.errorMessage = errorMessage;
        this.error = error;
    }

    public static HealthCheckResult unhealthy(String errorMessage, Throwable error) {
        return new HealthCheckResult(false, errorMessage, error);
    }

    public static HealthCheckResult unhealthy(Throwable error) {
        return new HealthCheckResult(false, null, error);
    }

    public static HealthCheckResult unhealthy(String errorMessage) {
        return new HealthCheckResult(false, errorMessage, null);
    }

    public static HealthCheckResult healthy() {
        return new HealthCheckResult(true,null, null);
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public boolean isUnhealthy() {
        return !isHealthy;
    }

    public Optional<String> getErrorMessage() {
        if (errorMessage != null) {
            return Optional.of(errorMessage);
        } else if (error != null) {
            return Optional.of(error.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }

}
