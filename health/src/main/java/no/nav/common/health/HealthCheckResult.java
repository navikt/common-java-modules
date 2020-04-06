package no.nav.common.health;

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

    public static HealthCheckResult healthy() {
        return new HealthCheckResult(true,null, null);
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Throwable getError() {
        return error;
    }

}
