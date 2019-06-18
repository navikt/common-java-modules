package no.nav.apiapp.security.veilarbabac;

import no.nav.apiapp.security.PepClient;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.SubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.NavAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.rest.RestUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.WebTarget;
import java.util.Optional;
import java.util.function.Supplier;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.apiapp.util.UrlUtils.clusterUrlForApplication;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class VeilarbAbacPepClient implements Helsesjekk, Cloneable {

    private static final String PATH_PERSON = "person";
    private static final String PATH_ENHET = "veilarbenhet";
    private static final String PATH_PING = "ping";

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
    private String veilarbacOverstyrtRessurs;
    private Builder endringsbygger;

    private VeilarbAbacPepClient() {
    }

    public static Builder ny() {
        return new Builder();
    }

    public Builder endre() {
        if(endringsbygger==null) {
            throw new IllegalStateException("Kan ikke endre VeilarbPepClient. Mangler endringsbygger");
        }

        return endringsbygger;
    }

    public void sjekkLesetilgangTilBruker(Bruker bruker) {
        new TilgangssjekkBruker()
                .metrikkLogger(logger,ACTION_READ,bruker::getAktoerId)
                .veilarbAbacFnrSjekker(() -> harVeilarbAbacTilgang(PATH_PERSON, ACTION_READ, IDTYPE_FNR, bruker.getFoedselsnummer()))
                .veilarbAbacAktoerIdSjekker(() -> harVeilarbAbacTilgang(PATH_PERSON, ACTION_READ, IDTYPE_AKTOR_ID, bruker.getAktoerId()))
                .abacFnrSjekker(() -> pepClient.sjekkLeseTilgangTilFnr(bruker.getFoedselsnummer()))
                .foretrekkVeilarbAbac(foretrekkVeilarbAbacSupplier.get())
                .brukAktoerId(brukAktoerIdSupplier.get())
                .sammenliknTilgang(sammenliknTilgangSupplier.get())
                .sjekkTilgangTilBruker();
    }

    public void sjekkSkrivetilgangTilBruker(Bruker bruker) {
        new TilgangssjekkBruker()
                .metrikkLogger(logger,ACTION_UPDATE, bruker::getAktoerId)
                .veilarbAbacFnrSjekker(() -> harVeilarbAbacTilgang(PATH_PERSON, ACTION_UPDATE, IDTYPE_FNR, bruker.getFoedselsnummer()))
                .veilarbAbacAktoerIdSjekker(() -> harVeilarbAbacTilgang(PATH_PERSON, ACTION_UPDATE, IDTYPE_AKTOR_ID, bruker.getAktoerId()))
                .abacFnrSjekker(() -> pepClient.sjekkSkriveTilgangTilFnr(bruker.getFoedselsnummer()))
                .foretrekkVeilarbAbac(foretrekkVeilarbAbacSupplier.get())
                .brukAktoerId(brukAktoerIdSupplier.get())
                .sammenliknTilgang(sammenliknTilgangSupplier.get())
                .sjekkTilgangTilBruker();
    }

    public boolean harTilgangTilEnhet(String enhetId) {

        return new TilgangssjekkEnhet()
                .metrikkLogger(logger,ACTION_READ,()->enhetId)
                .veilarbAbacSjekker(() -> harVeilarbAbacTilgang(PATH_ENHET, ACTION_READ, IDTYPE_ENHET_ID, enhetId))
                .abacSjekker(() -> harAbacTilgangTilEnhet(enhetId))
                .foretrekkVeilarbAbac(foretrekkVeilarbAbacSupplier.get())
                .sammenliknTilgang(sammenliknTilgangSupplier.get())
                .sjekkTilgangTilEnhet();

    }

    @Override
    public void helsesjekk() {
        int status = RestUtils.withClient(c -> c.target(abacTargetUrl)
                .path(PATH_PING)
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

    private boolean harVeilarbAbacTilgang(String path, String action, String idType, String id) {
        return "permit".equals(RestUtils.withClient(c -> overstyrRessurs(c.target(abacTargetUrl)
                .path(path)
                .queryParam(idType, id)
                .queryParam("action", action))
                .request()
                .header(AUTHORIZATION, "Bearer " + systemUserTokenProvider.get())
                .header("subject", oidcTokenSupplier.get()
                        .orElseThrow(() -> new IllegalStateException("Mangler OIDC-token")))
                .get(String.class)
        ));
    }

    private WebTarget overstyrRessurs(WebTarget webTarget) {
        if(veilarbacOverstyrtRessurs !=null) {
            return webTarget.queryParam("resource", veilarbacOverstyrtRessurs);
        } else {
            return webTarget;
        }
    }

    /**
     * Bygger VeilarbabacPepClient
     *
     * Krever at følgende metoder blir kalt før bygg():
     * - medPep()
     * - medSystemUserTokenProvider()
     *
     * Standard oppførsel er
     * - bruker-autorisering: Abac blir kalt med fnr
     * - enhet-autorisering: Abac blir kalt med enhetId
     *
     * Annen oppførsel kan konfigureres med toggle-suppliers:
     * - A: brukAktoerId(...) Hvis supplier gir true, så brukes aktørId i stedet for fnr. Betyr implisitt kall til Veilarbabac
     * - S: sammenlikneTilgang(...) Hvis supplier gir true , sammenlikne (hvis relevant) resultatene fra Abac og Veilarbabac og logg evt forskjell
     * - V: foretrekkVeilarbAbacResultat(...): Hvis supplier gir true og det foreligger resultat fra både Abac og Veilarbabac, fortrekk Veilarbabac
     *
     * Effekt av feature toggels for bruker-autorisering:
     * - (Ingen):   Abac kalles med fnr
     * - A:         Veilarbabac kalles med aktørId
     * - A+S:       Veilarbabac kalles med aktørId, deretter med fnr. Avvik i resultat logges. Fnr-resultat foretrekkes dersom det foreligger
     * - A+V:       (Samme som A)
     * - A+S+V:     (Samme som A+S)
     * - S:         Veilarbabac og Abac kalles med fnr. Avvik i resultat logges. Abac-resultat foretrekkes dersom det foreligger
     * - S+V:       Veilarbabac og Abac kalles med fnr. Avvik i resultat logges. Veilarbabac-resultat foretrekkes dersom det foreligger
     * - V:         Veilarbabac kalles med fnr.
     *
     * Effekt av feature toggles for enhet-autorisering:
     * - (Ingen):   Abac kalles med enhetId
     * - A:         Abac kalles med enhetId
     * - A+S:       (Samme som S)
     * - A+V:       (Samme som V)
     * - A+S+V:     (Samme som S+V)
     * - S:         Veilarbabac og Abac kalles med enhetId. Avvik i resultat logges. Abac-resultat foretrekkes
     * - S+V:       Veilarbabac og Abac kalles med enhet. Avvik i resultat logges. Veilarbabac-resultat foretrekkes
     * - V:         Veilarbabac kalles med enhetId.
     **/
    public static class Builder implements Cloneable {

        private VeilarbAbacPepClient veilarbAbacPepClient = new VeilarbAbacPepClient();
        private Optional<String> veilarbAbacUrl = Optional.empty();
        private ResourceType resourceType = ResourceType.VeilArbPerson;

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

        /**
         * NavAttributter.RESOURCE_VEILARB_PERSON er standard ressurs mot Abac, og implisitt i Veilarbabac
         * Kall denne for å bruke NavAttributter.RESOURCE_FELLES_PERSON i stedet
         * @return Builder
         */
        public Builder medResourceTypePerson() {
            this.resourceType = ResourceType.Person;
            veilarbAbacPepClient.veilarbacOverstyrtRessurs = NavAttributter.RESOURCE_FELLES_PERSON;
            return this;
        }

        /**
         * NavAttributter.RESOURCE_VEILARB_PERSON er standard ressurs mot Abac, og implisitt i Veilarbabac
         * Kall denne for å bruke NavAttributter.RESOURCE_VEILARB_UNDER_OPPFOLGING i stedet
         * @return Builder
         */
        public Builder medResourceTypeUnderOppfolging() {
            this.resourceType = ResourceType.VeilArbUnderOppfolging;
            veilarbAbacPepClient.veilarbacOverstyrtRessurs = NavAttributter.RESOURCE_VEILARB_UNDER_OPPFOLGING;
            return this;
        }

        public VeilarbAbacPepClient bygg() {
            if (veilarbAbacPepClient.systemUserTokenProvider == null) {
                throw new IllegalStateException("SystemUserTokenProvider er ikke satt");
            }

            if (this.pep == null) {
                throw new IllegalStateException("Pep er ikke satt");
            }

            veilarbAbacPepClient.pepClient = new PepClient(pep, "veilarb", resourceType);

            veilarbAbacPepClient.abacTargetUrl = veilarbAbacUrl
                        .orElseGet(() -> clusterUrlForApplication("veilarbabac"));

            try {
                Builder endringsbygger =  (Builder)this.clone();
                endringsbygger.veilarbAbacPepClient = (VeilarbAbacPepClient) veilarbAbacPepClient.clone();
                veilarbAbacPepClient.endringsbygger = endringsbygger;

            } catch(CloneNotSupportedException e) {
                throw new IllegalStateException("Klarte ikke å klone for endringsbygging",e);
            }

            return veilarbAbacPepClient;
        }
    }

}
