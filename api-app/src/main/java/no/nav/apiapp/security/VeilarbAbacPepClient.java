package no.nav.apiapp.security;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.apiapp.util.StringUtils;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.SubjectHandler;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action;
import no.nav.sbl.rest.RestUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.apiapp.util.StringUtils.nullOrEmpty;
import static no.nav.apiapp.util.UrlUtils.clusterUrlForApplication;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class VeilarbAbacPepClient implements Helsesjekk {

    private Logger logger = getLogger(VeilarbAbacPepClient.class);

    private Supplier<Boolean> brukAktoerIdSupplier = () -> false;
    private Supplier<Boolean> sammenliknTilgangSupplier = () -> false;
    private Supplier<String> systemUserTokenProvider;
    private Supplier<Optional<String>> oidcTokenSupplier = () -> SubjectHandler.getSsoToken(SsoToken.Type.OIDC);

    private String abacTargetUrl;
    private PepClient pepClient;

    private VeilarbAbacPepClient() {
    }

    public static Builder ny() {
        return new Builder();
    }

    public void sjekkLesetilgangTilBruker(Bruker bruker) {
        new Tilgangssjekk(bruker, Action.ActionId.READ, b -> pepClient.sjekkLeseTilgangTilFnr(b.getFoedselsnummer()))
                .sjekkTilgangTilBruker();
    }

    public void sjekkSkrivetilgangTilBruker(Bruker bruker) {

        new Tilgangssjekk(bruker, Action.ActionId.WRITE, b -> pepClient.sjekkSkriveTilgangTilFnr(b.getFoedselsnummer()))
                .sjekkTilgangTilBruker();
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


    private boolean harVeilarbAbacTilgangTilAktoerId(Bruker bruker, Action.ActionId action) {
        return harVeilarbAbacTilgangTilBruker(bruker, action, "aktorId", Bruker::getAktoerId);
    }

    private boolean harVeilarbAbacTilgangTilFnr(Bruker bruker, Action.ActionId action) {
        return harVeilarbAbacTilgangTilBruker(bruker, action, "fnr", Bruker::getFoedselsnummer);

    }

    private boolean harVeilarbAbacTilgangTilBruker(Bruker bruker, Action.ActionId action, String idType, Function<Bruker, String> brukerMapper) {
        return "permit".equals(RestUtils.withClient(c -> c.target(abacTargetUrl)
                .path("person")
                .queryParam(idType, brukerMapper.apply(bruker))
                .queryParam("action", action.getId())
                .request()
                .header(AUTHORIZATION, "Bearer " + systemUserTokenProvider.get())
                .header("subject", oidcTokenSupplier.get()
                        .orElseThrow(() -> new IllegalStateException("Mangler OIDC-token")))
                .get(String.class)
        ));
    }

    public static class Builder {

        VeilarbAbacPepClient veilarbAbacPepClient = new VeilarbAbacPepClient();
        private Optional<String> veilarbAbacUrl = Optional.empty();
        private Pep pep;

        public Builder brukAktoerId(Supplier<Boolean> featureToggleSupplier) {
            veilarbAbacPepClient.brukAktoerIdSupplier = featureToggleSupplier;
            return this;
        }

        public Builder sammenlikneTilgang(Supplier<Boolean> featureToggleSupplier) {
            veilarbAbacPepClient.sammenliknTilgangSupplier = featureToggleSupplier;
            return this;
        }

        public Builder medPep(Pep pep) {
            this.pep = pep;
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

        public Builder medOidcTokenProvider(Supplier<Optional<String>> oidcTokenSupplier) {
            veilarbAbacPepClient.oidcTokenSupplier = oidcTokenSupplier;
            return this;
        }

        public VeilarbAbacPepClient bygg() {
            if (veilarbAbacPepClient.systemUserTokenProvider == null) {
                throw new IllegalStateException("SystemUserTokenProvider er ikke satt");
            }
            if (this.pep == null) {
                throw new IllegalStateException("Pep er ikke satt");
            }

            veilarbAbacPepClient.pepClient = new PepClient(pep, "veilarb", ResourceType.VeilArbPerson);

            veilarbAbacPepClient.abacTargetUrl = veilarbAbacUrl
                    .orElseGet(() -> clusterUrlForApplication("veilarbabac"));

            return veilarbAbacPepClient;
        }
    }

    public static class Bruker {

        private String fnr;
        private String aktoerId;
        private Function<String, String> aktoerIdTilFnr;
        private Function<String, String> fnrTilAktoerId ;

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

            public Builder medAktoerIdTilFoedselsnummerKonvertering(Function<String,String> aktoerIdTilFnr) {
                bruker.aktoerIdTilFnr=aktoerIdTilFnr;
                return this;
            }

            public Builder medFoedselnummerTilAktoerIdKonvertering(Function<String,String> fnrTilAktoerId) {
                bruker.fnrTilAktoerId=fnrTilAktoerId;
                return this;
            }

            public Bruker bygg() {
                if ((nullOrEmpty(bruker.fnr) && nullOrEmpty(bruker.aktoerId))) {
                    throw new IllegalStateException("Bruker mangler både fødselsnummer og aktørId");
                }

                if (nullOrEmpty(bruker.fnr) && bruker.aktoerIdTilFnr==null) {
                    throw new IllegalStateException("Bruker mangler fødselsnummer og konverterer fra aktørId");
                }

                if (nullOrEmpty (bruker.aktoerId) && bruker.fnrTilAktoerId==null) {
                    throw new IllegalStateException("Bruker mangler aktørId og konverterer fra fnr");
                }

                return bruker;
            }

        }

        public String getFoedselsnummer() {
            if(fnr==null) {
                fnr = aktoerIdTilFnr.apply(aktoerId);
            }
            return fnr;
        }

        public String getAktoerId() {
            if(aktoerId==null) {
                aktoerId = fnrTilAktoerId.apply(fnr);
            }
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

    private class Tilgangssjekk {

        private final Bruker bruker;
        private final Action.ActionId action;
        private final Consumer<Bruker> abacSjekker;
        private final MetrikkSkriver metrikkSkriver = new MetrikkSkriver();

        private Tilgangssjekk(Bruker bruker, Action.ActionId action, Consumer<Bruker> abacSjekker) {
            this.bruker = bruker;
            this.action = action;
            this.abacSjekker = abacSjekker;
        }

        void sjekkTilgangTilBruker() {

            Boolean brukAktoerId = brukAktoerIdSupplier.get();
            Boolean sammenliknTilgang = sammenliknTilgangSupplier.get();

            boolean harTilgang;

            if (brukAktoerId && sammenliknTilgang) {
                harTilgang = sjekkOgSammenliknTilgangForAktoerIdOgFnr();
            } else if (sammenliknTilgang) {
                harTilgang = sjekkOgSammenliknTilgangForFnr();
            } else if (brukAktoerId) {
                harTilgang = sjekkTilgangTilAktoerId();
            } else {
                harTilgang = sjekkAbacTilgangTilFnr();
            }

            metrikkSkriver.skrivMetrikk(brukAktoerId,action);

            if (!harTilgang) {
                throw new IngenTilgang();
            }
        }

        private boolean sjekkTilgangTilAktoerId() {
            return harVeilarbAbacTilgangTilAktoerId(bruker, action);
        }

        private boolean sjekkOgSammenliknTilgangForFnr() {
            Boolean veilarbAbacResultat=null;

            try {
                veilarbAbacResultat = harVeilarbAbacTilgangTilFnr(bruker, action);
            } catch (Throwable e) {
                // Ignorer feilen. Vi kjører videre med Abac direkte
                logger.error("Kall mot veilarbAbac feiler", e);
            }

            boolean abacResultat = sjekkAbacTilgangTilFnr();

            if (veilarbAbacResultat!=null && abacResultat != veilarbAbacResultat) {
                registrerAvvik();
            }

            return abacResultat;
        }

        private boolean sjekkOgSammenliknTilgangForAktoerIdOgFnr() {
            boolean fnrResultat = harVeilarbAbacTilgangTilFnr(bruker, action);
            boolean aktoerIdResultat = harVeilarbAbacTilgangTilAktoerId(bruker, action);

            if (fnrResultat != aktoerIdResultat) {
                registrerAvvik();
            }

            // Stoler mest på fnr-resultatet
            return fnrResultat;
        }

        private boolean sjekkAbacTilgangTilFnr() {
            try {
                abacSjekker.accept(bruker);
                return true;
            } catch (IngenTilgang e) {
                return false;
            }
        }

        private void registrerAvvik() {
            logger.warn("Fikk avvik i tilgang for %s", bruker.getAktoerId());

            metrikkSkriver.erAvvik = true;
        }
    }

    private class MetrikkSkriver {

        private final MeterRegistry meterRegistry = MetricsFactory.getMeterRegistry();

        private boolean erAvvik = false;

        private void skrivMetrikk(Boolean brukAktoerId, Action.ActionId action) {
            meterRegistry.counter("veilarabac-abac-pep",
                    "brukerId",
                    brukAktoerId ? "aktoerId" : "fnr",
                    "identType",
                    SubjectHandler.getIdentType().map(Enum::name).orElse("unknown"),
                    "action",
                    action.name(),
                    "avvik",
                    Boolean.toString(erAvvik)
            ).increment();
        }

    }


}
