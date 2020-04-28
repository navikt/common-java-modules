package no.nav.common.abac;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AbacHealthCheck implements HealthCheck {

    private final String abacUrl;

    private final OkHttpClient client;

    public AbacHealthCheck(String abacUrl) {
        this.abacUrl = abacUrl;
        this.client = new OkHttpClient();
    }

    @Override
    public HealthCheckResult checkHealth() {
        Request request = new Request.Builder()
                .url(abacUrl)
                .get()
                .build();

        try(Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
               return HealthCheckResult.unhealthy("Received unexpected response from abac ping" + response.code());
            }

            return HealthCheckResult.healthy();
        } catch (Exception e) {
           return HealthCheckResult.unhealthy(e);
        }
    }

}
