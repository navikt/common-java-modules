package no.nav.apiapp.security;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.RequestData;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.Before;
import org.junit.Test;

import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.READ;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PepClientTest {

    private static final String FNR = "fnr";
    private static final String APPLICATION_DOMAIN = "test";
    private static final ResourceType RESOURCE_TYPE = ResourceType.values()[0];
    private BiasedDecisionResponse PERMIT = new BiasedDecisionResponse(Decision.Permit, new XacmlResponse());

    private final Pep pep = mock(Pep.class);
    private PepClient pepClient = new PepClient(pep, APPLICATION_DOMAIN, RESOURCE_TYPE);

    @Before
    public void setup() throws PepException {
        when(pep.nyRequest()).thenReturn(new RequestData());
    }

    @Test(expected = IngenTilgang.class)
    public void sjekkLeseTilgangTilFnr() throws PepException {
        pepClient.sjekkLeseTilgangTilFnr("fnr");
    }

    @Test
    public void sjekkLeseTilgangTilFnr_har_tilgang() throws PepException {
        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, READ, RESOURCE_TYPE)).thenReturn(PERMIT);
        assertThat(pepClient.sjekkLeseTilgangTilFnr(FNR)).isEqualTo(FNR);
    }

    @Test(expected = IngenTilgang.class)
    public void sjekkSkriveTilgangTilFnr() {
        pepClient.sjekkSkriveTilgangTilFnr("fnr");
    }

    @Test
    public void sjekkSkriveTilgangTilFnr_har_tilgang() throws PepException {
        when(pep.harInnloggetBrukerTilgangTilPerson(FNR, APPLICATION_DOMAIN, WRITE, RESOURCE_TYPE)).thenReturn(PERMIT);
        assertThat(pepClient.sjekkSkriveTilgangTilFnr("fnr")).isEqualTo(FNR);
    }

}