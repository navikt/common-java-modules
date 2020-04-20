package no.nav.common.health;

import java.util.List;
import java.util.Optional;

public class HealthChecker {

    public static void throwOnFirstFailing(List<HealthCheck> healthChecks) {
        findFirstFailingCheck(healthChecks).ifPresent((failedCheckResult) -> {
            throw new RuntimeException("Health check failed: " + failedCheckResult.getErrorMessage());
        });
    }

    public static Optional<HealthCheckResult> findFirstFailingCheck(List<HealthCheck> healthChecks) {
        for (HealthCheck healthCheck : healthChecks) {
            HealthCheckResult result = healthCheck.checkHealth();

            if (!result.isHealthy()) {
                return Optional.of(result);
            }
        }

        return Optional.empty();
    }

}
