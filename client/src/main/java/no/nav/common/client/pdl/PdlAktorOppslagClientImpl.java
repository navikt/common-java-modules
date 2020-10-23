package no.nav.common.client.pdl;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

@Slf4j
public class PdlAktorOppslagClientImpl implements PdlAktorOppslagClient {

    private final GraphqlRequestBuilder<HentIdentVariables> hentAktorIdRequestBuilder =
            new GraphqlRequestBuilder<>("pdl/hent-gjeldende-aktorid.graphql");


    private final GraphqlRequestBuilder<HentIdentVariables> hentFnrRequestBuilder =
            new GraphqlRequestBuilder<>("pdl/hent-gjeldende-aktorid.graphql");

    private final PdlClient pdlClient;

    public PdlAktorOppslagClientImpl(String pdlUrl, Supplier<String> userTokenSupplier, Supplier<String> consumerTokenSupplier) {
        this.pdlClient = new PdlClientImpl(pdlUrl, Tema.GEN, userTokenSupplier, consumerTokenSupplier);
    }

    public PdlAktorOppslagClientImpl(PdlClient pdlClient) {
        this.pdlClient = pdlClient;
    }

    @Override
    public Fnr hentFnr(AktorId aktorId) {
        HentIdenterResponse response = pdlClient.request(hentFnrRequestBuilder.buildRequest(new HentIdentVariables(aktorId.get())), HentIdenterResponse.class);

        if (response.getErrors() != null) {
            log.error("Henting av fnr fra PDL feilet: " + JsonUtils.toJson(response.getErrors()));
            throw new RuntimeException("Henting av fnr fra PDL feilet");
        }

        return ofNullable(response.getData())
                .map(data -> data.hentIdenter
                        .identer
                        .stream()
                        .findFirst()
                        .orElseThrow())
                .map(identData -> Fnr.of(identData.ident))
                .orElseThrow();
    }

    @Override
    public AktorId hentAktorId(Fnr fnr) {
        HentIdenterResponse response = pdlClient.request(hentAktorIdRequestBuilder.buildRequest(new HentIdentVariables(fnr.get())), HentIdenterResponse.class);

        if (response.getErrors() != null) {
            log.error("Henting av aktør id fra PDL feilet: " + JsonUtils.toJson(response.getErrors()));
            throw new RuntimeException("Henting av aktør id fra PDL feilet");
        }

        return ofNullable(response.getData())
                .map(data -> data.hentIdenter
                        .identer
                        .stream()
                        .findFirst()
                        .orElseThrow())
                .map(identData -> AktorId.of(identData.ident))
                .orElseThrow();
    }

    @Override
    public HealthCheckResult checkHealth() {
        return null;
    }

    @Value
    private static class HentIdentVariables {
        String ident;
    }

    static class HentIdenterResponse extends GraphqlResponse<HentIdenterResponse.HentIdenterResponseData> {

        static class HentIdenterResponseData {
            IdenterResponseData hentIdenter;

            static class IdenterResponseData {
                List<IdentData> identer;

                static class IdentData {
                    String ident;
                }
            }
        }

    }

}
