package no.nav.apiapp.security.veilarbabac;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.sbl.dialogarena.common.abac.pep.NavAttributter;
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

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.fail;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType.Person;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType.VeilArbPerson;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.READ;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.WRITE;
import static org.mockito.Mockito.*;

public class VeilarbAbacPepClientTest {

    private static final String FNR = "fnr";
    private static final String AKTOER_ID = "aktorId";
    private static final String ENHET_ID = "enhetId";
    private static final String APPLICATION_DOMAIN = "veilarb";
    private static final String SYSTEM_TOKEN = "token";
    public static final String OIDC_TOKEN = "OIDC-token";

    private static final String URL_REGEX_AKTOER_ID_WRITE = String.format("/person\\?aktorId=%s&action=update",AKTOER_ID);
    private static final String URL_REGEX_FNR_READ = String.format("/person\\?fnr=%s&action=read",FNR);
    private static final String URL_REGEX_AKTOER_ID_READ = String.format("/person\\?aktorId=%s&action=read",AKTOER_ID);
    private static final String URL_REGEX_ENHET_READ = String.format("/veilarbenhet\\?enhetId=%s&action=read",ENHET_ID);

    private BiasedDecisionResponse PERMIT = new BiasedDecisionResponse(Decision.Permit, new XacmlResponse());
    private BiasedDecisionResponse DENY = new BiasedDecisionResponse(Decision.Deny, new XacmlResponse());
    
    private static final Bruker BRUKER = Bruker.fraFnr(FNR).medAktoerId(AKTOER_ID);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private final Pep pep = mock(Pep.class);
    private final Logger logger = mock(Logger.class);
    public static final RequestData PEP_REQUEST_DATA_ENHET = new RequestData()
            .withResourceType(ResourceType.Enhet)
            .withDomain(APPLICATION_DOMAIN)
            .withEnhet(ENHET_ID);

    @Before
    public void setup() throws PepException {
        when(pep.nyRequest()).thenReturn(new RequestData());
     }

    @Test
    public void testAbacMedLesetilgang() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson)).thenReturn(PERMIT);

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger().bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson);
    }

    @Test(expected = IngenTilgang.class)
    public void testAbacUtenLesetilgang() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson)).thenReturn(DENY);

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger().bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson);
    }

    @Test
    public void testAbacMedSkrivetilgang() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, WRITE, VeilArbPerson)).thenReturn(PERMIT);

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger().bygg();

        veilarbAbacPepClient.sjekkSkrivetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, WRITE, VeilArbPerson);

    }

    @Test
    public void testVeilarbAbacMedLesetilgangForAktoerId() {
        lagVeilarbAbacResponse(URL_REGEX_AKTOER_ID_READ, 200, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .brukAktoerId(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_AKTOER_ID_READ)));
    }

    @Test
    public void testVeilarbAbacMedSkrivetilgangForAktoerId()  {
        lagVeilarbAbacResponse(URL_REGEX_AKTOER_ID_WRITE, 200, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .brukAktoerId(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkSkrivetilgangTilBruker(BRUKER);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_AKTOER_ID_WRITE)));
    }

    @Test(expected = IngenTilgang.class)
    public void testVeilarbAbacUtenLesetilgangForAktoerId()  {

        lagVeilarbAbacResponse(URL_REGEX_AKTOER_ID_READ, 200, "hallo");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .brukAktoerId(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);
    }

    @Test
    public void testSammenliknAbacOgVeilarbabacMedLesetilgangTilFnr() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson)).thenReturn(PERMIT);

        lagVeilarbAbacResponse(URL_REGEX_FNR_READ, 200, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_FNR_READ)));
        Mockito.verify(logger,times(0)).warn("Fikk avvik i tilgang for {}",AKTOER_ID);

    }

    @Test
    public void testSammenliknAbacOgVeilarbabacMedLesetilgangTilFnrOgUlikRespons() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson)).thenReturn(PERMIT);

        lagVeilarbAbacResponse(URL_REGEX_FNR_READ, 200, "hallo");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_FNR_READ)));
        Mockito.verify(logger,times(1)).warn("Fikk avvik i tilgang for {}",AKTOER_ID);
    }

    @Test
    public void testSammenliknAbacOgVeilarbabacMedLesetilgangTilFnrOgAbacGirDeny() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson)).thenReturn(DENY);

        String urlRegex = URL_REGEX_FNR_READ;

        lagVeilarbAbacResponse(urlRegex, 200, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .bygg();

        try {
            veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);
            fail("Forventet IngenTilgang-exception");
        } catch(IngenTilgang e) { }

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(urlRegex)));
        Mockito.verify(logger,times(1)).warn("Fikk avvik i tilgang for {}",AKTOER_ID);
    }

    @Test
    public void testSammenliknAktoerIdOgFnrVeilarbabacMedLesetilgangFnrOk() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson)).thenReturn(DENY);

        lagVeilarbAbacResponse(URL_REGEX_FNR_READ, 200, "permit");
        lagVeilarbAbacResponse(URL_REGEX_AKTOER_ID_READ, 200, "deny");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .brukAktoerId(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(0)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_FNR_READ)));
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_AKTOER_ID_READ)));
        Mockito.verify(logger,times(1)).warn("Fikk avvik i tilgang for {}",AKTOER_ID);
    }

    @Test
    public void testSammenliknAktoerIdOgFnrVeilarbabacMedLesetilgangBeggeFeiler_FallbackTilAbac() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson)).thenReturn(PERMIT);

        lagVeilarbAbacResponse(URL_REGEX_FNR_READ, 400, "feil");
        lagVeilarbAbacResponse(URL_REGEX_AKTOER_ID_READ, 400, "feil");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .brukAktoerId(()->true)
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_FNR_READ)));
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_AKTOER_ID_READ)));
        Mockito.verify(logger,times(1)).warn("Fikk avvik i tilgang for {}",AKTOER_ID);
    }

    @Test
    public void testSammenliknAktoerIdOgFnrVeilarbabacMedLesetilgangFnrIkkeOk() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson)).thenReturn(DENY);

        lagVeilarbAbacResponse(URL_REGEX_FNR_READ, 200, "deny");
        lagVeilarbAbacResponse(URL_REGEX_AKTOER_ID_READ, 200, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .brukAktoerId(()->true)
                .bygg();

        try {
            veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);
            fail("Forventet IngenTilgang-exception");
        } catch(IngenTilgang e) { }

        Mockito.verify(pep,times(0)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, VeilArbPerson);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_FNR_READ)));
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_AKTOER_ID_READ)));
        Mockito.verify(logger,times(1)).warn("Fikk avvik i tilgang for {}",AKTOER_ID);
    }

    @Test
    public void testSammenliknAbacOgVeilarbabacMedEnhet() throws PepException {

        when(pep.harTilgang(PEP_REQUEST_DATA_ENHET)).thenReturn(PERMIT);

        lagVeilarbAbacResponse(URL_REGEX_ENHET_READ, 200, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .bygg();

        assertTrue(veilarbAbacPepClient.harTilgangTilEnhet(ENHET_ID));
        Mockito.verify(pep,times(1)).harTilgang(PEP_REQUEST_DATA_ENHET);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_ENHET_READ)));
        Mockito.verify(logger,times(0)).warn("Fikk avvik i tilgang for {}",ENHET_ID);

    }

    @Test
    public void testSammenliknAbacOgVeilarbabacMedEnhetUlikResonse() throws PepException {

        when(pep.harTilgang(PEP_REQUEST_DATA_ENHET)).thenReturn(DENY);

        lagVeilarbAbacResponse(URL_REGEX_ENHET_READ, 200, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .bygg();

        assertFalse(veilarbAbacPepClient.harTilgangTilEnhet(ENHET_ID));
        Mockito.verify(pep,times(1)).harTilgang(PEP_REQUEST_DATA_ENHET);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_ENHET_READ)));
        Mockito.verify(logger,times(1)).warn("Fikk avvik i tilgang for {}",ENHET_ID);
    }

    @Test
    public void testSammenliknAbacOgVeilarbabacMedEnhetUlikResonseOGVeilarbAbacForetrukket() throws PepException {

        when(pep.harTilgang(PEP_REQUEST_DATA_ENHET)).thenReturn(DENY);

        lagVeilarbAbacResponse(URL_REGEX_ENHET_READ, 200, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .sammenlikneTilgang(()->true)
                .foretrekkVeilarbAbacResultat(()->true)
                .bygg();

        assertTrue(veilarbAbacPepClient.harTilgangTilEnhet(ENHET_ID));
        Mockito.verify(pep,times(1)).harTilgang(PEP_REQUEST_DATA_ENHET);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(URL_REGEX_ENHET_READ)));
        Mockito.verify(logger,times(1)).warn("Fikk avvik i tilgang for {}",ENHET_ID);
    }

    @Test
    public void testAbacMedLesetilgangMedOverstyrtRessurs() throws PepException {

        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, Person)).thenReturn(PERMIT);

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .medResourceTypePerson()
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        Mockito.verify(pep,times(1)).harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, Person);
    }

    @Test
    public void testVeilarbAbacMedOverstyrtRessurs() {
        String urlRegexAktoerIdReadOverstyrt = URL_REGEX_AKTOER_ID_READ
                + "&resource="+NavAttributter.RESOURCE_FELLES_PERSON;
        lagVeilarbAbacResponse(urlRegexAktoerIdReadOverstyrt, 200, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .brukAktoerId(()->true)
                .medResourceTypePerson()
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);

        WireMock.verify(1, newRequestPattern(GET,urlMatching(urlRegexAktoerIdReadOverstyrt)));
    }

    @Test
    public void testEndring() {
        String urlRegexAktoerIdReadOverstyrtPerson = URL_REGEX_AKTOER_ID_READ
                + "&resource="+NavAttributter.RESOURCE_FELLES_PERSON;

        String urlRegexAktoerIdReadOverstyrtUnderOppfolging = URL_REGEX_AKTOER_ID_READ
                + "&resource="+NavAttributter.RESOURCE_VEILARB_UNDER_OPPFOLGING;

        lagVeilarbAbacResponse(urlRegexAktoerIdReadOverstyrtPerson, 200,"permit");
        lagVeilarbAbacResponse(urlRegexAktoerIdReadOverstyrtUnderOppfolging, 200, "permit");

        VeilarbAbacPepClient veilarbAbacPepClient = lagBygger()
                .brukAktoerId(()->true)
                .medResourceTypePerson()
                .bygg();

        VeilarbAbacPepClient veilarbAbacPepClient2 = veilarbAbacPepClient
                .endre()
                .medResourceTypeUnderOppfolging()
                .bygg();

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(urlRegexAktoerIdReadOverstyrtPerson)));
        WireMock.verify(0, newRequestPattern(GET,urlMatching(urlRegexAktoerIdReadOverstyrtUnderOppfolging)));

        veilarbAbacPepClient2.sjekkLesetilgangTilBruker(BRUKER);
        WireMock.verify(1, newRequestPattern(GET,urlMatching(urlRegexAktoerIdReadOverstyrtPerson)));
        WireMock.verify(1, newRequestPattern(GET,urlMatching(urlRegexAktoerIdReadOverstyrtUnderOppfolging)));

        veilarbAbacPepClient.sjekkLesetilgangTilBruker(BRUKER);
        WireMock.verify(2, newRequestPattern(GET,urlMatching(urlRegexAktoerIdReadOverstyrtPerson)));
        WireMock.verify(1, newRequestPattern(GET,urlMatching(urlRegexAktoerIdReadOverstyrtUnderOppfolging)));
    }

    private void lagVeilarbAbacResponse(String pathRegex, int statuskode, String response) {
        givenThat(get(urlMatching(pathRegex))
                .withHeader("Authorization", matching("Bearer "+ SYSTEM_TOKEN))
                .withHeader("subject",matching(OIDC_TOKEN))
                .willReturn(aResponse()
                        .withStatus(statuskode)
                        .withBody(response)
                )
        );
    }

    private VeilarbAbacPepClient.Builder lagBygger() {
        return VeilarbAbacPepClient.ny()
                .medPep(pep)
                .medLogger(logger)
                .medSystemUserTokenProvider(() -> SYSTEM_TOKEN)
                .medOidcTokenProvider(()-> Optional.of(OIDC_TOKEN))
                .medVeilarbAbacUrl("http://localhost:"+wireMockRule.port());
    }
}