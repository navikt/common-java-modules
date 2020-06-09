package no.nav.common.client.norg2;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.List;

import static no.nav.common.rest.client.RestUtils.*;
import static no.nav.common.utils.UrlUtils.joinPaths;

@Slf4j
public class NorgHttp2Client implements Norg2Client {

    private final String norg2Url;

    private final OkHttpClient client;

    public NorgHttp2Client(String norg2Url) {
        this.norg2Url = norg2Url;
        this.client = RestClient.baseClient();
    }

    public NorgHttp2Client(String norg2Url, OkHttpClient client) {
        this.norg2Url = norg2Url;
        this.client = client;
    }

    @Override
    @SneakyThrows
    public List<Enhet> alleAktiveEnheter() {
        Request request = new Request.Builder()
                .url(joinPaths(norg2Url, "/api/v1/enhet?enhetStatusListe=AKTIV"))
                .build();

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return parseJsonResponseArrayOrThrow(response, Enhet.class);
        }
    }

    @Override
    @SneakyThrows
    public Enhet hentEnhet(String enhetId) {
        Request request = new Request.Builder()
                .url(joinPaths(norg2Url, "/api/v1/enhet/", enhetId))
                .build();

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return parseJsonResponseOrThrow(response, Enhet.class);
        }
    }

    @Override
    @SneakyThrows
    public Enhet hentTilhorendeEnhet(String geografiskOmrade) {
        Request request = new Request.Builder()
                .url(joinPaths(norg2Url, "/api/v1/enhet/navkontor/", geografiskOmrade))
                .build();

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return parseJsonResponseOrThrow(response, Enhet.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(norg2Url, "/internal/isAlive"), client);
    }

}
