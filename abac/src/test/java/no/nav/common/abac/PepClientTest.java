package no.nav.common.abac;

import no.nav.common.abac.domain.ResourceType;
import no.nav.common.abac.domain.response.BiasedDecisionResponse;
import no.nav.common.abac.domain.response.Decision;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.types.feil.IngenTilgang;
import org.junit.Before;
import org.junit.Test;

import static no.nav.common.abac.domain.request.Action.ActionId.READ;
import static no.nav.common.abac.domain.request.Action.ActionId.WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class PepClientTest {

    private static final AbacPersonId FNR = AbacPersonId.fnr("fnr");
    private static final String APPLICATION_DOMAIN = "test";
    private static final ResourceType RESOURCE_TYPE = ResourceType.values()[0];
    private BiasedDecisionResponse PERMIT = new BiasedDecisionResponse(Decision.Permit, new XacmlResponse());
    private BiasedDecisionResponse DENY = new BiasedDecisionResponse(Decision.Deny, new XacmlResponse());

    private final Pep pep = mock(Pep.class);
    private PepClient pepClient = new PepClient(pep, APPLICATION_DOMAIN, RESOURCE_TYPE);

    @Before
    public void setup() {
        when(pep.nyRequest()).thenReturn(new RequestData());
    }

    @Test(expected = IngenTilgang.class)
    public void sjekkLeseTilgangTilFnr() {
        pepClient.sjekkLesetilgangTilFnr("fnr");
    }

    @Test
    public void sjekkLeseTilgangTilFnr_har_tilgang() {
        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE)).thenReturn(PERMIT);
        assertThat(pepClient.sjekkLesetilgangTilFnr(FNR.getId())).isEqualTo(FNR.getId());
    }

    @Test(expected = IngenTilgang.class)
    public void sjekkSkriveTilgangTilFnr() {
        pepClient.sjekkSkrivetilgangTilFnr("fnr");
    }

    @Test
    public void sjekkSkriveTilgangTilFnr_har_tilgang() {
        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, WRITE, RESOURCE_TYPE)).thenReturn(PERMIT);
        assertThat(pepClient.sjekkSkrivetilgangTilFnr("fnr")).isEqualTo(FNR.getId());
    }

    @Test
    public void harTilgangTilEnhet_tilgang() {
        when(pep.harTilgang(any(RequestData.class))).thenReturn(PERMIT);
        assertTrue(pepClient.harTilgangTilEnhet("enhet"));
    }

    @Test
    public void harTilgangTilEnhet_ingen_tilgang() {
        when(pep.harTilgang(any(RequestData.class))).thenReturn(DENY);
        assertFalse(pepClient.harTilgangTilEnhet("enhet"));
    }

    @Test
    public void harTilgangTilEnhet_enhet_null_konverteres_til_tom_streng() {
        when(pep.harTilgang(any(RequestData.class))).thenReturn(DENY);
        assertFalse(pepClient.harTilgangTilEnhet(null));
        verify(pep).harTilgang((RequestData) argThat(x -> ((RequestData) x).getEnhet().equals("")));
    }

}
