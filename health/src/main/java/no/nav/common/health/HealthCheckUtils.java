package no.nav.common.health;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class HealthCheckUtils {

    public static HealthCheckResult pingUrl(String url, OkHttpClient client) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return HealthCheckResult.unhealthy(format("Helsesjekk feilet mot %s. Fikk uventet status %d", url, response.code()));
            }

            return HealthCheckResult.healthy();
        } catch (Exception e) {
            return HealthCheckResult.unhealthy("Helsesjekk feilet mot " + url, e);
        }
    }

    public static HealthCheckResult safeCheckHealth(HealthCheck healthCheck) {
        try {
            return healthCheck.checkHealth();
        } catch (Exception e) {
            return HealthCheckResult.unhealthy("Failed to check health", e);
        }
    }

    public static void throwOnFirstFailing(List<HealthCheck> healthChecks) {
        findFirstFailingCheck(healthChecks).ifPresent((failedCheckResult) -> {
            throw new RuntimeException("Health check failed: " + failedCheckResult.getErrorMessage());
        });
    }

    public static Optional<HealthCheckResult> findFirstFailingCheck(List<HealthCheck> healthChecks) {
        for (HealthCheck healthCheck : healthChecks) {
            HealthCheckResult result = safeCheckHealth(healthCheck);

            if (!result.isHealthy()) {
                return Optional.of(result);
            }
        }

        return Optional.empty();
    }

}
