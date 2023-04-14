package no.nav.common.rest.client;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class RestClient {

    private static OkHttpClient client;

    public static OkHttpClient baseClient() {
        if (client == null) {
            client = baseClientBuilder().build();
        }

        return client;
    }

    public static OkHttpClient.Builder baseClientBuilder() {
        return new OkHttpClient.Builder()
                .addInterceptor(new LogRequestInterceptor())
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .followRedirects(false);
    }

    public static void setBaseClient(OkHttpClient baseClient) {
        if (client != null) {
            throw new IllegalStateException("Base client is already initialized");
        }

        client = baseClient;
    }
}
