package no.nav.common.client.axsys;

import jakarta.ws.rs.core.HttpHeaders;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.rest.client.RestUtils.createBearerToken;
import static no.nav.common.rest.client.RestUtils.throwIfNotSuccessful;
import static no.nav.common.utils.UrlUtils.joinPaths;

abstract class BaseAxsysClient implements AxsysClient {
    private final OkHttpClient client;
    private final String axsysUrl;
    private final AxsysApi apiVersion;
    private final Supplier<String> serviceTokenSupplier;


    public BaseAxsysClient(String axsysUrl, AxsysApi apiVersion, Supplier<String> serviceTokenSupplier) {
        this.axsysUrl = axsysUrl;
        this.apiVersion = apiVersion;
        this.serviceTokenSupplier = serviceTokenSupplier;
        this.client = RestClient.baseClient();
    }

    public BaseAxsysClient(String axsysUrl, AxsysApi apiVersion, Supplier<String> serviceTokenSupplier, OkHttpClient client) {
        this.axsysUrl = axsysUrl;
        this.apiVersion = apiVersion;
        this.serviceTokenSupplier = serviceTokenSupplier;
        this.client = client;
    }

    @SneakyThrows
    public List<NavIdent> hentAnsatte(EnhetId enhetId) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(axsysUrl, "api/" + apiVersion.path + "/enhet/" + enhetId + "/brukere"))
                .header(HttpHeaders.ACCEPT, MEDIA_TYPE_JSON.toString())
                .header(AUTHORIZATION, getBearerToken())
                .build();

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            List<AxsysEnhetBruker> brukere = RestUtils.parseJsonResponseArrayOrThrow(response, AxsysEnhetBruker.class);
            return brukere.stream().map(AxsysEnhetBruker::getAppIdent).collect(Collectors.toList());
        }
    }

    @SneakyThrows
    public List<AxsysEnhet> hentTilganger(NavIdent navIdent) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(axsysUrl, "api/" + apiVersion.path + "/tilgang/" + navIdent.get()))
                .header(HttpHeaders.ACCEPT, MEDIA_TYPE_JSON.toString())
                .header(AUTHORIZATION, getBearerToken())
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

    private @NotNull String getBearerToken() {
        if (serviceTokenSupplier == null) {
            return createBearerToken("");
        }
        return createBearerToken(serviceTokenSupplier.get());
    }

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    static class AxsysEnheter {
        List<AxsysEnhet> enheter;
    }

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    static class AxsysEnhetBruker {
        private NavIdent appIdent;
        private String historiskIdent;
    }

    enum AxsysApi {
        V1("v1"), V2("v2");

        public final String path;

        AxsysApi(String path) {
            this.path = path;
        }
    }
}
