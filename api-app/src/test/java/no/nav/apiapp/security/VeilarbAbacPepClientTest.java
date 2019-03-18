package no.nav.apiapp.security;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.RequestData;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static junit.framework.TestCase.fail;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.READ;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.WRITE;
import static org.mockito.Mockito.*;

public class VeilarbAbacPepClientTest {

    private static final String FNR = "fnr";
    private static final String AKTOER_ID = "aktorId";
    private static final String APPLICATION_DOMAIN = "veilarb";
    private static final ResourceType RESOURCE_TYPE = ResourceType.Person;
    private static final String TOKEN = "token";

    private static final String URL_REGEX_AKTOER_ID_WRITE = String.format("/person\\?aktorId=%s&action=update",AKTOER_ID);
    private static final String URL_REGEX_FNR_READ = String.format("/person\\?fnr=%s&action=read",FNR);
    private static final String URL_REGEX_AKTOER_ID_READ = String.format("/person\\?aktorId=%s&action=read",AKTOER_ID);

    private BiasedDecisionResponse PERMIT = new BiasedDecisionResponse(Decision.Permit, new XacmlResponse());
    private BiasedDecisionResponse DENY = new BiasedDecisionResponse(Decision.Deny, new XacmlResponse());


    private static final VeilarbAbacPepClient.Bruker BRUKER = VeilarbAbacPepClient.Bruker.ny()
            .medFoedeselsnummer(FNR)
            .medAktoerId(AKTOER_ID)
            .bygg();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private final Pep pep = mock(Pep.class);
    private final Logger logger = mock(Logger.class);

    @Before
    public void setup() throws PepException {
        when(pep.nyRequest()).thenReturn(new RequestData());
     }

    @Test
    public void testAbacMedLesetilgang() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE)).thenReturn(PERMIT);

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger().bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE);
    }

    @Test(expected = IngenTilgang.class)
    public void testAbacUtenLesetilgang() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE)).thenReturn(DENY);

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger().bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE);
    }

    @Test
    public void testAbacMedSkrivetilgang() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, WRITE, RESOURCE_TYPE)).thenReturn(PERMIT);

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger().bygg();

        veilarbAbacPepClient.sjekkSkrivetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, WRITE, RESOURCE_TYPE);

    }

    @Test
    public void testVeilarbAbacMedLesetilgangForAktoerId() {
        lagVeilarbAbacResponse(URL_REGEX_AKTOER_ID_READ, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .brukAktoerId(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_AKTOER_ID_READ)));
    }

    @Test
    public void testVeilarbAbacMedSkrivetilgangForAktoerId()  {
        lagVeilarbAbacResponse(URL_REGEX_AKTOER_ID_WRITE, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .brukAktoerId(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkSkrivetilgangTilBruker(BRUKER);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_AKTOER_ID_WRITE)));
    }

    @Test(expected = IngenTilgang.class)
    public void testVeilarbAbacUtenLesetilgangForAktoerId()  {

        lagVeilarbAbacResponse(URL_REGEX_AKTOER_ID_READ, "hallo");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .brukAktoerId(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);
    }

    @Test
    public void testSammenliknAbacOgVeilarbabacMedLesetilgangTilFnr() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE)).thenReturn(PERMIT);

        lagVeilarbAbacResponse(URL_REGEX_FNR_READ, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_FNR_READ)));
        Mockito.verify(logger,times(0)).warn("Fikk avvik i tilgang for %s",BRUKER);

    }

    @Test
    public void testSammenliknAbacOgVeilarbabacMedLesetilgangTilFnrOgUlikRespons() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE)).thenReturn(PERMIT);

        lagVeilarbAbacResponse(URL_REGEX_FNR_READ, "hallo");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_FNR_READ)));
        Mockito.verify(logger,times(1)).warn("Fikk avvik i tilgang for %s",BRUKER);
    }

    @Test
    public void testSammenliknAbacOgVeilarbabacMedLesetilgangTilFnrOgAbacGirDeny() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE)).thenReturn(DENY);

        String urlRegex = URL_REGEX_FNR_READ;

        lagVeilarbAbacResponse(urlRegex, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .bygg();

        try {
            veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);
            fail("Forventet IngenTilgang-exception");
        } catch(IngenTilgang e) { }

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(urlRegex)));
        Mockito.verify(logger,times(1)).warn("Fikk avvik i tilgang for %s",BRUKER);
    }

    @Test
    public void testSammenliknAktoerIdOgFnrVeilarbabacMedLesetilgangFnrOk() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE)).thenReturn(DENY);

        lagVeilarbAbacResponse(URL_REGEX_FNR_READ, "permit");
        lagVeilarbAbacResponse(URL_REGEX_AKTOER_ID_READ, "deny");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .brukAktoerId(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(0)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_FNR_READ)));
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_AKTOER_ID_READ)));
        Mockito.verify(logger,times(1)).warn("Fikk avvik i tilgang for %s",BRUKER);
    }

    @Test
    public void testSammenliknAktoerIdOgFnrVeilarbabacMedLesetilgangFnrIkkeOk() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE)).thenReturn(DENY);

        lagVeilarbAbacResponse(URL_REGEX_FNR_READ, "deny");
        lagVeilarbAbacResponse(URL_REGEX_AKTOER_ID_READ, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .brukAktoerId(()->true)
                .bygg();

        try {
            veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);
            fail("Forventet IngenTilgang-exception");
        } catch(IngenTilgang e) { }

        Mockito.verify(pep,times(0)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_FNR_READ)));
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_AKTOER_ID_READ)));
        Mockito.verify(logger,times(1)).warn("Fikk avvik i tilgang for %s",BRUKER);
    }

    private void lagVeilarbAbacResponse(String pathRegex, String response) {
        givenThat(get(urlMatching(pathRegex))
                .withHeader("Authorization", matching("Bearer "+TOKEN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(response)
                )
        );
    }

    private VeilarbAbacPepClient.Builder lagBygger() {
        return VeilarbAbacPepClient.ny()
                .medPep(pep)
                .medLogger(logger)
                .medSystemUserTokenProvider(() -> TOKEN)
                .medVeilarbAbacUrl("http://localhost:"+wireMockRule.port());
    }
}