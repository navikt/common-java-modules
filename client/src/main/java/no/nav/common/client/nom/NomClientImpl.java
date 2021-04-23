package no.nav.common.client.nom;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.common.client.utils.graphql.GraphqlUtils;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.CollectionUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.common.log.LogUtils.runWithMDCContext;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.rest.client.RestUtils.createBearerToken;
import static no.nav.common.utils.UrlUtils.joinPaths;

@Slf4j
public class NomClientImpl implements NomClient {

    private final static int NOM_MAX_BATCH_SIZE = 100;

    private final GraphqlRequestBuilder<RessursQuery.Variables> ressurserQueryRequestBuilder =
            new GraphqlRequestBuilder<>("nom/ressurser.graphql");

    private final String nomApiUrl;

    private final OkHttpClient client;

    private final Supplier<String> serviceTokenSupplier;

    public NomClientImpl(String nomApiUrl, Supplier<String> serviceTokenSupplier) {
        this.nomApiUrl = nomApiUrl;
        this.serviceTokenSupplier = serviceTokenSupplier;
        this.client = RestClient.baseClient();
    }

    public NomClientImpl(String nomApiUrl, Supplier<String> serviceTokenSupplier, OkHttpClient client) {
        this.nomApiUrl = nomApiUrl;
        this.serviceTokenSupplier = serviceTokenSupplier;
        this.client = client;
    }

    @Override
    public VeilederNavn finnNavn(NavIdent navIdent) {
        List<VeilederNavn> veilederNavn = finnNavn(Collections.singletonList(navIdent));

        if (veilederNavn.isEmpty()) {
            throw new IllegalStateException("Fant ikke navn for NAV-ident: " + navIdent);
        }

        return veilederNavn.get(0);
    }

    @Override
    public List<VeilederNavn> finnNavn(List<NavIdent> navIdenter) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return CollectionUtils.partition(navIdenter, NOM_MAX_BATCH_SIZE)
                .parallelStream()
                .flatMap((identBatch) -> runWithMDCContext(contextMap, () -> hentNavnTilIdenter(identBatch).stream()))
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private List<VeilederNavn> hentNavnTilIdenter(List<NavIdent> navIdenter) {
        GraphqlRequest<RessursQuery.Variables> gqlRequest = ressurserQueryRequestBuilder.buildRequest(new RessursQuery.Variables(navIdenter));

        Request request = new Request.Builder()
                .url(joinPaths(nomApiUrl, "/graphql"))
                .header(ACCEPT, MEDIA_TYPE_JSON.toString())
                .header(AUTHORIZATION, createBearerToken(serviceTokenSupplier.get()))
                .post(RequestBody.create(MEDIA_TYPE_JSON, JsonUtils.toJson(gqlRequest)))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);

            String gqlJsonResponse = RestUtils.getBodyStr(response)
                    .orElseThrow(() -> new IllegalStateException("Body is missing from NOM response"));

            RessursQuery.Response graphqlResponse = JsonUtils.fromJson(gqlJsonResponse, RessursQuery.Response.class);

            GraphqlUtils.logWarningIfError(graphqlResponse);
            GraphqlUtils.throwIfMissingData(graphqlResponse);

            return mapTilVeilederNavn(graphqlResponse);
        }
    }


    private List<VeilederNavn> mapTilVeilederNavn(RessursQuery.Response graphqlResponse) {
        List<RessursQuery.ResponseData.RessurserItem> ressurser = graphqlResponse.getData().ressurser;
        List<VeilederNavn> veilederNavnListe = new ArrayList<>(ressurser.size());

        ressurser.forEach(ressursItem -> {
            RessursQuery.ResponseData.RessurserItem.Ressurs ressurs = ressursItem.ressurs;

            if (ressurs == null || ressurs.person == null) {
                log.error("Fant ikke navn til veileder med ident: {}", ressursItem.id);
                return;
            }

            VeilederNavn veilederNavn = new VeilederNavn()
                    .setNavIdent(ressurs.navIdent)
                    .setFornavn(ressurs.person.navn.fornavn)
                    .setMellomnavn(ressurs.person.navn.mellomnavn)
                    .setEtternavn(ressurs.person.navn.etternavn);

            veilederNavnListe.add(veilederNavn);
        });

        return veilederNavnListe;
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(nomApiUrl, "/internal/health/liveness"), client);
    }

    @Data
    static class RessursQuery {
        @Value
        static class Variables {
            List<NavIdent> identer;
        }

        static class Response extends GraphqlResponse<ResponseData> {}

        @Data
        static class ResponseData {
            List<RessurserItem> ressurser;

            static class RessurserItem {
                String id;
                Ressurs ressurs; // Can be null

                static class Ressurs {
                    NavIdent navIdent;
                    Person person; // Can be null

                    @Data
                    static class Person {
                        Navn navn;

                        @Data
                        static class Navn {
                            String fornavn;
                            String mellomnavn; // Can be null
                            String etternavn;
                        }
                    }
                }
            }

        }
    }

}
