package no.nav.common.client.pdl;

import lombok.SneakyThrows;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.function.Supplier;

import static javax.ws.rs.core.HttpHeaders.*;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.rest.client.RestUtils.createBearerToken;
import static no.nav.common.utils.UrlUtils.joinPaths;

public class PdlClientImpl implements PdlClient {

    private static final Tema DEFAULT_TEMA = Tema.GEN;

    private final String pdlUrl;

    private final Tema pdlTema;

    private final OkHttpClient client;

    private final Supplier<String> userTokenSupplier;

    private final Supplier<String> consumerTokenSupplier;

    /**
     * @param pdlUrl URL til PDL
     * @param tema hvilket tema som skal benyttes i requests mot PDL
     * @param userTokenSupplier supplier for tokens som brukes i Authorization header. Kan enten være systembruker eller ekstern/intern.
     *                          OBS: Hvis systembruker token blir brukt så vil ikke PDL gjøre tilgangskontroll på requestet.
     * @param consumerTokenSupplier supplier av systembruker tokens for applikasjonen som gjør requests mot PDL
     */
    public PdlClientImpl(String pdlUrl, Tema tema, Supplier<String> userTokenSupplier, Supplier<String> consumerTokenSupplier) {
        this.pdlUrl = pdlUrl;
        this.pdlTema = tema;
        this.userTokenSupplier = userTokenSupplier;
        this.consumerTokenSupplier = consumerTokenSupplier;
        this.client = RestClient.baseClient();
    }

    public PdlClientImpl(String pdlUrl, Supplier<String> userTokenSupplier, Supplier<String> consumerTokenSupplier) {
        this(pdlUrl, DEFAULT_TEMA, userTokenSupplier, consumerTokenSupplier);
    }

    @Override
    @SneakyThrows
    public String rawRequest(String gqlRequestJson) {
        Request request = new Request.Builder()
                .url(joinPaths(pdlUrl, "/graphql"))
                .header(ACCEPT, MEDIA_TYPE_JSON.toString())
                .header(CONTENT_TYPE, MEDIA_TYPE_JSON.toString())
                .header(AUTHORIZATION, createBearerToken(userTokenSupplier.get()))
                .header("Nav-Consumer-Token", createBearerToken(consumerTokenSupplier.get()))
                .header("Tema", pdlTema.name())
                .post(RequestBody.create(MEDIA_TYPE_JSON, gqlRequestJson))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .orElseThrow(() -> new IllegalStateException("Body is missing from PDL response"));
        }
    }

    @Override
    public <D> GraphqlResponse<D> request(GraphqlRequest<?> graphqlRequest, Class<D> dataClass) {
        String graphqlJsonResponse = rawRequest(JsonUtils.toJson(graphqlRequest));


        return null;
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(pdlUrl, "/internal/health/liveness"), client);
    }

}
