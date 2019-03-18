package no.nav.apiapp.security;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action;
import no.nav.sbl.dialogarena.common.cxf.InstanceSwitcher;
import no.nav.sbl.rest.RestUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.apiapp.util.UrlUtils.clusterUrlForApplication;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class VeilarbAbacPepClient implements Helsesjekk {

    private Logger logger = getLogger(VeilarbAbacPepClient.class);

    private Supplier<Boolean> brukAktoerIdSupplier = () -> false;
    private Supplier<Boolean> sammenliknTilgangSupplier = () -> false;
    private Supplier<String> systemUserTokenProvider;

    private String abacTargetUrl;
    private Pep pep;

    private VeilarbAbacPepClient() {
    }

    public static Builder ny() {
        return new Builder();
    }

    public void sjekkLesetilgangTilBruker(Bruker bruker) {
        sjekkTilgangTilBruker(bruker, Action.ActionId.READ, fnr->lagPepClient().sjekkLeseTilgangTilFnr(fnr));
    }

    public void sjekkSkrivetilgangTilBruker(Bruker bruker) {

        sjekkTilgangTilBruker(bruker,Action.ActionId.WRITE,fnr->lagPepClient().sjekkSkriveTilgangTilFnr(fnr));
    }

    @Override
    public void helsesjekk() {
        int status = RestUtils.withClient(c -> c.target(abacTargetUrl)
                .path("ping")
                .request()
                .get()
                .getStatus());

        if (status != 200) {
            throw new IllegalStateException("Rest kall mot veilarbabac feilet");
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "veilarbabac helsesjekk",
                abacTargetUrl,
                "Sjekker om veilarbabac endepunkt svarer",
                true
        );
    }


    private void sjekkTilgangTilBruker(Bruker bruker, Action.ActionId action, Consumer<String> pepClientSjekk) {
        if(brukAktoerIdSupplier.get() && sammenliknTilgangSupplier.get()) {
            boolean fnrResultat = harVeilarbAbacTilgangTilFnr(bruker,action);
            boolean aktoerIdResultat = harVeilarbAbacTilgangTilAktoerId(bruker,action);

            if(fnrResultat!=aktoerIdResultat) {
                loggAvvik(bruker);
            }

            // Stoler mest på fnr-resultatet
            if(!fnrResultat) {
                throw new IngenTilgang();
            }
        } else if (sammenliknTilgangSupplier.get()) {
            boolean fnrResultat = harVeilarbAbacTilgangTilFnr(bruker,action);

            try {
                pepClientSjekk.accept(bruker.getFnr());
            } catch(IngenTilgang e) {
                if(fnrResultat) {
                    loggAvvik(bruker);
                }

                throw e;
            }

            if(!fnrResultat) {
                loggAvvik(bruker);
            }

        } else if(brukAktoerIdSupplier.get()) {
            if(!harVeilarbAbacTilgangTilAktoerId(bruker,action)) {
                throw new IngenTilgang();
            }
        } else {
            pepClientSjekk.accept(bruker.getFnr());
        }
    }

    private void loggAvvik(Bruker bruker) {
        logger.warn("Fikk avvik i tilgang for %s", bruker);
    }

    private PepClient lagPepClient() {
        return new PepClient(pep, "veilarb",ResourceType.Person);
    }

    private boolean harVeilarbAbacTilgangTilAktoerId(Bruker bruker, Action.ActionId action) {
        return harVeilarbAbacTilgangTilBruker(bruker,action, "aktorId", Bruker::getAktoerId);
    }

    private boolean harVeilarbAbacTilgangTilFnr(Bruker bruker, Action.ActionId action) {
        return harVeilarbAbacTilgangTilBruker(bruker,action, "fnr", Bruker::getFnr);

    }

    private boolean harVeilarbAbacTilgangTilBruker(Bruker bruker, Action.ActionId action, String idType, Function<Bruker,String> brukerMapper) {
        return "permit".equals(RestUtils.withClient(c -> c.target(abacTargetUrl)
                .path("person")
                .queryParam(idType, brukerMapper.apply(bruker))
                .queryParam("action", action.getId())
                .request()
                .header(AUTHORIZATION, "Bearer " + systemUserTokenProvider.get())
                .get(String.class)
        ));
    }

    public static class Builder {

        VeilarbAbacPepClient veilarbAbacPepClient = new VeilarbAbacPepClient();
        private Optional<String> veilarbAbacUrl = Optional.empty();

        public Builder brukAktoerId(Supplier<Boolean> featureToggleSupplier) {
            veilarbAbacPepClient.brukAktoerIdSupplier = featureToggleSupplier;
            return this;
        }

        public Builder sammenlikneTilgang(Supplier<Boolean> featureToggleSupplier) {
            veilarbAbacPepClient.sammenliknTilgangSupplier = featureToggleSupplier;
            return this;
        }

        public Builder medPep(Pep pep) {
            veilarbAbacPepClient.pep = pep;
            return this;
        }

        public Builder medVeilarbAbacUrl(String url) {
            veilarbAbacUrl = Optional.of(url);
            return this;
        }

        public Builder medLogger(Logger logger) {
            veilarbAbacPepClient.logger = logger;
            return this;
        }

        public Builder medSystemUserTokenProvider(Supplier<String> systemUserTokenProvider) {
            veilarbAbacPepClient.systemUserTokenProvider = systemUserTokenProvider;
            return this;
        }

        public VeilarbAbacPepClient bygg() {
            if(veilarbAbacPepClient.systemUserTokenProvider==null) {
                throw new IllegalStateException("SystemUserTokenProvider er ikke satt");
            }
            if(veilarbAbacPepClient.pep==null) {
                throw new IllegalStateException("Pep er ikke satt");
            }

            veilarbAbacPepClient.abacTargetUrl = veilarbAbacUrl
                    .orElseGet(()->clusterUrlForApplication("veilarbabac"));

            return veilarbAbacPepClient;
        }
    }

    public static class Bruker {

        private String fnr;
        private String aktoerId;

        public static Builder ny() {
            return new Builder();
        }

        public static class Builder {

            Bruker bruker = new Bruker();

            public Builder medFoedeselsnummer(String fnr) {
                bruker.fnr = fnr;
                return this;
            }

            public Builder medAktoerId(String aktoerId) {
                bruker.aktoerId = aktoerId;
                return this;
            }

            public Bruker bygg() {

                if(bruker.fnr==null || bruker.fnr.length()==0 || bruker.aktoerId==null || bruker.aktoerId.length()==0) {
                    throw new IllegalStateException("Bruker mangler fødselsnummer og aktørId");
                }

                return bruker;
            }

        }

        public String getFnr() {
            return fnr;
        }

        public String getAktoerId() {
            return aktoerId;
        }

        @Override
        public String toString() {
            return "Bruker{" +
                    "fnr='" + fnr + '\'' +
                    ", aktoerId='" + aktoerId + '\'' +
                    '}';
        }
    }
}
