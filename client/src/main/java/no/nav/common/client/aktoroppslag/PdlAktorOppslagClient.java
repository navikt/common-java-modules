package no.nav.common.client.aktoroppslag;

import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.client.pdl.Tema;
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.common.client.utils.graphql.GraphqlUtils;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
public class PdlAktorOppslagClient implements AktorOppslagClient {

    private static final String IDENT_GRUPPE_AKTORID = "AKTORID";

    private static final String IDENT_GRUPPE_FOLKEREGISTERIDENT = "FOLKEREGISTERIDENT";

    private final GraphqlRequestBuilder<HentIdentVariables> hentAktorIdRequestBuilder =
            new GraphqlRequestBuilder<>("pdl/hent-gjeldende-aktorid.graphql");

    private final GraphqlRequestBuilder<HentIdentVariables> hentFnrRequestBuilder =
            new GraphqlRequestBuilder<>("pdl/hent-gjeldende-fnr.graphql");

    private final GraphqlRequestBuilder<HentIdentVariables> hentIdenterMedHistorikkRequestBuilder =
            new GraphqlRequestBuilder<>("pdl/hent-identer-med-hist.graphql");

    private final GraphqlRequestBuilder<HentIdentBolkVariables<?>> hentIdentBolkRequestBuilder =
            new GraphqlRequestBuilder<>("pdl/hent-gjeldende-ident-bolk.graphql");

    private final PdlClient pdlClient;

    public PdlAktorOppslagClient(String pdlUrl, Supplier<String> userTokenSupplier, Supplier<String> consumerTokenSupplier) {
        this.pdlClient = new PdlClientImpl(pdlUrl, Tema.GEN, userTokenSupplier, consumerTokenSupplier);
    }

    public PdlAktorOppslagClient(String pdlUrl, Supplier<String> azureAdTokenSupplier) {
        this.pdlClient = new PdlClientImpl(pdlUrl, Tema.GEN, azureAdTokenSupplier, null);
    }

    public PdlAktorOppslagClient(PdlClient pdlClient) {
        this.pdlClient = pdlClient;
    }

    @Override
    public Fnr hentFnr(AktorId aktorId) {
        HentIdenterResponse response = pdlClient.request(
                hentFnrRequestBuilder.buildRequest(new HentIdentVariables(aktorId.get())),
                HentIdenterResponse.class
        );

        GraphqlUtils.throwIfErrorOrMissingData(response);

        return response.getData()
                .hentIdenter
                .identer
                .stream()
                .findFirst()
                .map(identData -> Fnr.of(identData.ident))
                .orElseThrow(IngenGjeldendeIdentException::new);
    }

    @Override
    public AktorId hentAktorId(Fnr fnr) {
        HentIdenterResponse response = pdlClient.request(
                hentAktorIdRequestBuilder.buildRequest(new HentIdentVariables(fnr.get())),
                HentIdenterResponse.class
        );

        GraphqlUtils.throwIfErrorOrMissingData(response);

        return response.getData()
                .hentIdenter
                .identer
                .stream()
                .findFirst()
                .map(identData -> AktorId.of(identData.ident))
                .orElseThrow();
    }

    @Override
    public Map<AktorId, Fnr> hentFnrBolk(List<AktorId> aktorIdListe) {
        HentIdenterBolkResponse response = pdlClient.request(
                hentIdentBolkRequestBuilder.buildRequest(new HentIdentBolkVariables<>(aktorIdListe)),
                HentIdenterBolkResponse.class
        );

        GraphqlUtils.throwIfErrorOrMissingData(response);

        return hentAlleIdentPairFraBolk(response.getData())
                .collect(Collectors.toMap(IdentPair::getAktorId, IdentPair::getFnr));
    }

    @Override
    public Map<Fnr, AktorId> hentAktorIdBolk(List<Fnr> fnrListe) {
        HentIdenterBolkResponse response = pdlClient.request(
                hentIdentBolkRequestBuilder.buildRequest(new HentIdentBolkVariables<>(fnrListe)),
                HentIdenterBolkResponse.class
        );

        GraphqlUtils.throwIfErrorOrMissingData(response);

        return hentAlleIdentPairFraBolk(response.getData())
                .collect(Collectors.toMap(IdentPair::getFnr, IdentPair::getAktorId));
    }

    @Override
    public BrukerIdenter hentIdenter(EksternBrukerId brukerId) {
        HentIdenterResponse response = pdlClient.request(
                hentIdenterMedHistorikkRequestBuilder.buildRequest(new HentIdentVariables(brukerId.get())),
                HentIdenterResponse.class
        );

        GraphqlUtils.throwIfErrorOrMissingData(response);

        List<HentIdenterResponse.HentIdenterResponseData.IdenterResponseData.IdentData> identer =
                response.getData().hentIdenter.identer;

        List<HentIdenterResponse.HentIdenterResponseData.IdenterResponseData.IdentData> folkeregisteridenter =
                identer.stream().filter(ident -> IDENT_GRUPPE_FOLKEREGISTERIDENT.equals(ident.gruppe)).collect(toList());

        List<HentIdenterResponse.HentIdenterResponseData.IdenterResponseData.IdentData> aktorIder =
                identer.stream().filter(ident -> IDENT_GRUPPE_AKTORID.equals(ident.gruppe)).collect(toList());

        return new BrukerIdenter(
                folkeregisteridenter.stream().filter(ident -> !ident.historisk).findFirst().map(ident -> Fnr.of(ident.ident)).orElseThrow(),
                aktorIder.stream().filter(ident -> !ident.historisk).findFirst().map(ident -> AktorId.of(ident.ident)).orElseThrow(),
                folkeregisteridenter.stream().filter(ident -> ident.historisk).map(ident -> Fnr.of(ident.ident)).collect(toList()),
                aktorIder.stream().filter(ident -> ident.historisk).map(ident -> AktorId.of(ident.ident)).collect(toList())
        );
    }

    private static Stream<IdentPair> hentAlleIdentPairFraBolk(HentIdenterBolkResponse.HentIdenterBolkResponseData bolkResponseData) {
        return bolkResponseData
                .hentIdenterBolk
                .stream()
                .filter(identData -> identData.identer != null)
                .map(identData -> mergeIdenterTilPair(identData.identer));
    }

    private static IdentPair mergeIdenterTilPair(List<HentIdenterBolkResponse.HentIdenterBolkResponseData.IdenterResponseData.IdentData> brukerIdenter) {
        IdentPair identPair = new IdentPair();

        brukerIdenter
                .forEach(ident -> {
                    switch (ident.gruppe) {
                        case IDENT_GRUPPE_FOLKEREGISTERIDENT:
                            identPair.setFnr(Fnr.of(ident.ident));
                            break;
                        case IDENT_GRUPPE_AKTORID:
                            identPair.setAktorId(AktorId.of(ident.ident));
                            break;
                    }
                });

        return identPair;
    }

    @Override
    public HealthCheckResult checkHealth() {
        return pdlClient.checkHealth();
    }

    @Data
    private static class IdentPair {
        AktorId aktorId;
        Fnr fnr;
    }

    @Value
    private static class HentIdentVariables {
        String ident;
    }

    @Value
    private static class HentIdentBolkVariables<T extends EksternBrukerId> {
        List<T> identer;
    }

    protected static class HentIdenterResponse extends GraphqlResponse<HentIdenterResponse.HentIdenterResponseData> {
        private static class HentIdenterResponseData {
            IdenterResponseData hentIdenter;

            private static class IdenterResponseData {
                List<IdenterResponseData.IdentData> identer;

                private static class IdentData {
                    String ident;
                    String gruppe;
                    Boolean historisk;
                }
            }
        }
    }


    protected static class HentIdenterBolkResponse extends GraphqlResponse<HentIdenterBolkResponse.HentIdenterBolkResponseData> {
        private static class HentIdenterBolkResponseData {
            List<IdenterResponseData> hentIdenterBolk;

            private static class IdenterResponseData {
                List<IdentData> identer;

                private static class IdentData {
                    String ident;
                    String gruppe;
                }
            }
        }
    }

}
