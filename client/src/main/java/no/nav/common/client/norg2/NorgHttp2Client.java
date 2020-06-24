package no.nav.common.client.norg2;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.health.selftest.HealthCheckMetadata;
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

    private final boolean erKritisk;

    public NorgHttp2Client(String norg2Url, boolean erKritisk) {
        this.norg2Url = norg2Url;
        this.client = RestClient.baseClient();
        this.erKritisk = erKritisk;
    }

    public NorgHttp2Client(String norg2Url, OkHttpClient client, boolean erKritisk) {
        this.norg2Url = norg2Url;
        this.client = client;
        this.erKritisk = erKritisk;
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

    @Override
    public HealthCheckMetadata healthCheckMetadata() {
        return new HealthCheckMetadata("Norg2", erKritisk, joinPaths(norg2Url, "/internal/isAlive"));
    }
}
