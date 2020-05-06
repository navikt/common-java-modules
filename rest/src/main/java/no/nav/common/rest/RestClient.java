package no.nav.common.rest;

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
                .addInterceptor(new LogInterceptor())
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(false);
    }

}
