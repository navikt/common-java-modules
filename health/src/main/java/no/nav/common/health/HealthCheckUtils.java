package no.nav.common.health;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

}
