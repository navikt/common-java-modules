package no.nav.common.client.axsys;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
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

import javax.ws.rs.core.HttpHeaders;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
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
                .header(HttpHeaders.ACCEPT, MEDIA_TYPE_JSON.toString())
                .build();

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            List<AxsysEnhetBruker> brukere = RestUtils.parseJsonResponseArrayOrThrow(response, AxsysEnhetBruker.class);
            return brukere.stream().map(AxsysEnhetBruker::getAppIdent).collect(Collectors.toList());
        }
    }

    @SneakyThrows
    public List<AxsysEnhet> hentTilganger(NavIdent veileder) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(axsysUrl, "api/v1/tilgang/" + veileder.get()))
                .header(HttpHeaders.ACCEPT, MEDIA_TYPE_JSON.toString())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, AxsysEnheter.class).getEnheter();
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(axsysUrl, "/internal/isAlive"), client);
    }

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    static class AxsysEnheter {
        List<AxsysEnhet> enheter;
    }
}
