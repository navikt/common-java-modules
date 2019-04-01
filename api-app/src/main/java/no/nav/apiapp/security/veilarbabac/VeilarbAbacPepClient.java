package no.nav.apiapp.security.veilarbabac;

import no.nav.apiapp.security.PepClient;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.SubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.rest.RestUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.apiapp.util.UrlUtils.clusterUrlForApplication;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class VeilarbAbacPepClient implements Helsesjekk {

    private static final String RESOURCE_PERSON = "person";
    private static final String RESOURCE_ENHET = "enhet";
    private static final String RESOURCE_PING = "ping";

    private static final String ACTION_READ = "read";
    private static final String ACTION_UPDATE = "update";

    private static final String IDTYPE_FNR = "fnr";
    private static final String IDTYPE_AKTOR_ID = "aktorId";
    private static final String IDTYPE_ENHET_ID = "enhetId";

    private Logger logger = getLogger(VeilarbAbacPepClient.class);

    private Supplier<Boolean> brukAktoerIdSupplier = () -> false;
    private Supplier<Boolean> sammenliknTilgangSupplier = () -> false;
    private Supplier<Boolean> foretrekkVeilarbAbacSupplier = () -> false;
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
        new TilgangssjekkBruker()
                .metrikkLogger(logger,ACTION_READ,bruker::getAktoerId)
                .veilarbAbacFnrSjekker(() -> harVeilarbAbacTilgang(RESOURCE_PERSON, ACTION_READ, IDTYPE_FNR, bruker.getFoedselsnummer()))
                .veilarbAbacAktoerIdSjekker(() -> harVeilarbAbacTilgang(RESOURCE_PERSON, ACTION_READ, IDTYPE_AKTOR_ID, bruker.getAktoerId()))
                .abacFnrSjekker(() -> pepClient.sjekkLeseTilgangTilFnr(bruker.getFoedselsnummer()))
                .foretrekkVeilarbAbac(foretrekkVeilarbAbacSupplier.get())
                .brukAktoerId(brukAktoerIdSupplier.get())
                .sammenliknTilgang(sammenliknTilgangSupplier.get())
                .sjekkTilgangTilBruker();
    }

    public void sjekkSkrivetilgangTilBruker(Bruker bruker) {
        new TilgangssjekkBruker()
                .metrikkLogger(logger,ACTION_UPDATE, bruker::getAktoerId)
                .veilarbAbacFnrSjekker(() -> harVeilarbAbacTilgang(RESOURCE_PERSON, ACTION_UPDATE, IDTYPE_FNR, bruker.getFoedselsnummer()))
                .veilarbAbacAktoerIdSjekker(() -> harVeilarbAbacTilgang(RESOURCE_PERSON, ACTION_UPDATE, IDTYPE_AKTOR_ID, bruker.getAktoerId()))
                .abacFnrSjekker(() -> pepClient.sjekkSkriveTilgangTilFnr(bruker.getFoedselsnummer()))
                .foretrekkVeilarbAbac(foretrekkVeilarbAbacSupplier.get())
                .brukAktoerId(brukAktoerIdSupplier.get())
                .sammenliknTilgang(sammenliknTilgangSupplier.get())
                .sjekkTilgangTilBruker();
    }

    public boolean harTilgangTilEnhet(String enhetId) {

        return new TilgangssjekkEnhet()
                .metrikkLogger(logger,ACTION_READ,()->enhetId)
                .veilarbAbacSjekker(() -> harVeilarbAbacTilgang(RESOURCE_ENHET, ACTION_READ, IDTYPE_ENHET_ID, enhetId))
                .abacSjekker(() -> harAbacTilgangTilEnhet(enhetId))
                .foretrekkVeilarbAbac(foretrekkVeilarbAbacSupplier.get())
                .sammenliknTilgang(sammenliknTilgangSupplier.get())
                .sjekkTilgangTilEnhet();

    }

    @Override
    public void helsesjekk() {
        int status = RestUtils.withClient(c -> c.target(abacTargetUrl)
                .path(RESOURCE_PING)
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

    private boolean harAbacTilgangTilEnhet(String enhetId) {
        try {
            return pepClient.harTilgangTilEnhet(enhetId);
        } catch (PepException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean harVeilarbAbacTilgang(String resource, String action, String idType, String id) {
        return "permit".equals(RestUtils.withClient(c -> c.target(abacTargetUrl)
                .path(resource)
                .queryParam(idType, id)
                .queryParam("action", action)
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

        public Builder foretrekkVeilarbAbacResultat(Supplier<Boolean> featureToggleSupplier) {
            veilarbAbacPepClient.foretrekkVeilarbAbacSupplier = featureToggleSupplier;
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

}
