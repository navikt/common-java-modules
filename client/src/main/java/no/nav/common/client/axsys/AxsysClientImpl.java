package no.nav.common.client.axsys;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.UrlUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.List;
import java.util.stream.Collectors;

import static no.nav.common.rest.client.RestUtils.throwIfNotSuccessful;
import static no.nav.common.utils.UrlUtils.joinPaths;

public class AxsysClientImpl implements AxsysClient {
    private final OkHttpClient client;
    private final String axsysUrl;


    public AxsysClientImpl(String axsysUrl) {
        this.axsysUrl = axsysUrl;
        this.client = RestClient.baseClient();
    }

    public AxsysClientImpl(String axsysUrl, OkHttpClient client) {
        this.axsysUrl = axsysUrl;
        this.client = client;
    }

    @SneakyThrows
    public List<NavIdent> hentAnsatte(EnhetId enhetId) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(axsysUrl, "api/v1/enhet/" + enhetId + "/brukere"))
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            List<AxsysEnhetBruker> brukere = RestUtils.parseJsonResponseArrayOrThrow(response, AxsysEnhetBruker.class);
            return brukere.stream().map(AxsysEnhetBruker::getAppIdent).collect(Collectors.toList());
        }
    }

    @SneakyThrows
    public AxsysEnheter hentTilganger(NavIdent veileder) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(axsysUrl, "api/v1/tilgang/" + veileder.get()))
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, AxsysEnheter.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(axsysUrl, "/internal/isAlive"), client);
    }
}
