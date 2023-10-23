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

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
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

    private final String behandlingsnummer;

    /**
     * @param pdlUrl URL til PDL
     * @param tema hvilket tema som skal benyttes i requests mot PDL
     * @param userTokenSupplier supplier for tokens som brukes i Authorization header. Kan enten være systembruker eller ekstern/intern.
     *                          OBS: Hvis systembruker token blir brukt så vil ikke PDL gjøre tilgangskontroll på requestet.
     * @param consumerTokenSupplier supplier av systembruker tokens for applikasjonen som gjør requests mot PDL
     */
    public PdlClientImpl(String pdlUrl, Tema tema, Supplier<String> userTokenSupplier, Supplier<String> consumerTokenSupplier, String behandlingsnummer) {
        this.pdlUrl = pdlUrl;
        this.pdlTema = tema;
        this.userTokenSupplier = userTokenSupplier;
        this.consumerTokenSupplier = consumerTokenSupplier;
        this.client = RestClient.baseClient();
        this.behandlingsnummer = behandlingsnummer;
    }

    @Deprecated
    public PdlClientImpl(String pdlUrl, Tema tema, Supplier<String> userTokenSupplier, Supplier<String> consumerTokenSupplier) {
        this(pdlUrl, tema, userTokenSupplier, consumerTokenSupplier, null);
    }

    public PdlClientImpl(String pdlUrl, Supplier<String> userTokenSupplier, Supplier<String> consumerTokenSupplier, String behandlingsnummer) {
        this(pdlUrl, DEFAULT_TEMA, userTokenSupplier, consumerTokenSupplier, behandlingsnummer);
    }

    @Deprecated
    public PdlClientImpl(String pdlUrl, Supplier<String> userTokenSupplier, Supplier<String> consumerTokenSupplier) {
        this(pdlUrl, DEFAULT_TEMA, userTokenSupplier, consumerTokenSupplier, null);
    }

    public PdlClientImpl(String pdlUrl, Supplier<String> userTokenSupplier, String behandlingsnummer) {
        this(pdlUrl, DEFAULT_TEMA, userTokenSupplier, null, behandlingsnummer);
    }

    @Deprecated
    public PdlClientImpl(String pdlUrl, Supplier<String> userTokenSupplier) {
        this(pdlUrl, DEFAULT_TEMA, userTokenSupplier, null, null);
    }


    @Override
    @SneakyThrows
    public String rawRequest(String gqlRequestJson) {
        Request.Builder request = new Request.Builder()
                .url(joinPaths(pdlUrl, "/graphql"))
                .header(ACCEPT, MEDIA_TYPE_JSON.toString())
                .header(AUTHORIZATION, createBearerToken(userTokenSupplier.get()))
                .header("Tema", pdlTema.name())
                .header("behandlingsnummer", behandlingsnummer)
                .post(RequestBody.create(gqlRequestJson, MEDIA_TYPE_JSON));
        if (consumerTokenSupplier != null) {
            request.header("Nav-Consumer-Token", createBearerToken(consumerTokenSupplier.get()));
        }

        try (Response response = client.newCall(request.build()).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .orElseThrow(() -> new IllegalStateException("Body is missing from PDL response"));
        }
    }

    @Override
    public <D extends GraphqlResponse> D request(GraphqlRequest<?> graphqlRequest, Class<D> graphqlResponseClass) {
        String graphqlJsonResponse = rawRequest(JsonUtils.toJson(graphqlRequest));
        return JsonUtils.fromJson(graphqlJsonResponse, graphqlResponseClass);
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(pdlUrl, "/internal/health/liveness"), client);
    }

}
