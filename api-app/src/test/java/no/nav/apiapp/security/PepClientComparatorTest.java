package no.nav.apiapp.security;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.RequestData;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import org.junit.Test;
import org.slf4j.Logger;

import static no.nav.sbl.util.AssertUtils.assertTrue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PepClientComparatorTest {

    private static final String SYSTEM_TOKEN = "token";
    private final Pep pepOrginal = mock(Pep.class);
    private final Pep pepSammenligneMed = mock(Pep.class);
    private final VeilarbPepClient veilarbPepClient = new VeilarbPepClient(pepSammenligneMed);
    private final Logger log = mock(Logger.class);
    private final PepClientComparatorImpl comparator = new PepClientComparatorImpl(log);
    private final Bruker bruker = Bruker.fraFnr("fnr").medAktoerId("aktorId");
    private final AbacPersonId abacPersonId = AbacPersonId.fnr("fnr");
    private final String enhet = "enhet";

    VeilarbAbacPepClient.Builder builder = VeilarbAbacPepClient.ny()
            .medPep(pepOrginal)
            .brukAktoerId(() -> false)
            .medSystemUserTokenProvider(() -> SYSTEM_TOKEN)
            .medVeilarbAbacUrl("test");

    VeilarbAbacPepClient veilarbAbacPepClient = builder.bygg();

    @Test
    public void exceptionVedIngenTilgang__permitForBegge__sjekkLesetilgang() {
        pepOrginalPermit();
        pepSammenligneMedPermit();

        comparator.get(
                () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                () -> veilarbPepClient.sjekkLesetilgang(abacPersonId));

        forventIngenLogging();
    }

    @Test
    public void exceptionVedIngenTilgang__permitForBegge__sjekkSkrivegang() {
        pepOrginalPermit();
        pepSammenligneMedPermit();

        comparator.get(
                () -> veilarbAbacPepClient.sjekkSkrivetilgangTilBruker(bruker),
                () -> veilarbPepClient.sjekkSkrivetilgang(abacPersonId));

        forventIngenLogging();
    }

    @Test
    public void exceptionVedIngenTilgang__denyForSammenligneMed() {
        pepOrginalPermit();
        pepSammenligneMedDeny();

        comparator.get(
                () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                () -> veilarbPepClient.sjekkLesetilgang(abacPersonId));

        forventLoggetAvvik("Avvik i resultat fra pep sammenligning. Forventet tilgang fikk ingen tilgang");
    }

    @Test
    public void exceptionVedIngenTilgang__denyForOrginal() {
        pepOrginalDeny();
        pepSammenligneMedPermit();

        assertThatThrownBy(() ->
                comparator.get(
                        () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                        () -> veilarbPepClient.sjekkLesetilgang(abacPersonId)))
                .isInstanceOf(IngenTilgang.class);

        forventLoggetAvvik("Avvik i resultat fra pep sammenligning. Forventet ingen tilgang fikk tilgang");
    }

    @Test
    public void exceptionVedIngenTilgang__permitForOrginal__feilForSammenligneMed() {
        pepOrginalPermit();
        pepSammenligneMedFeil();

        comparator.get(
                () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                () -> veilarbPepClient.sjekkLesetilgang(abacPersonId));

        forventLoggetFeil();
    }

    @Test
    public void exceptionVedIngenTilgang__denyForOrginal__feilForSammenligneMed() {
        pepOrginalDeny();
        pepSammenligneMedFeil();

        assertThatThrownBy(() ->
                comparator.get(
                        () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                        () -> veilarbPepClient.sjekkLesetilgang(abacPersonId)))
                .isInstanceOf(IngenTilgang.class);

        forventLoggetFeil();
    }

    @Test
    public void exceptionVedIngenTilgang__feilForOrginal__permitForSammenligneMed() {
        pepOrginalMedFeil();
        pepSammenligneMedPermit();

        assertThatThrownBy(() ->
                comparator.get(
                        () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                        () -> veilarbPepClient.sjekkLesetilgang(abacPersonId)))
                .isInstanceOf(IngenTilgang.class);

        forventLoggetAvvik("Avvik i resultat fra pep sammenligning. Forventet ingen tilgang fikk tilgang");
    }

    @Test
    public void exceptionVedIngenTilgang__feilForOrginal__denyForSammenligneMed() {
        pepOrginalMedFeil();
        pepSammenligneMedDeny();

        assertThatThrownBy(() ->
                comparator.get(
                        () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                        () -> veilarbPepClient.sjekkLesetilgang(abacPersonId)))
                .isInstanceOf(IngenTilgang.class);

        forventIngenLogging();
    }

    @Test
    public void exceptionVedIngenTilgang__feilForOrginal__feilForSammenligneMed() {
        pepOrginalMedFeil();
        pepSammenligneMedFeil();

        assertThatThrownBy(() ->
                comparator.get(
                        () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                        () -> veilarbPepClient.sjekkLesetilgang(abacPersonId)))
                .isInstanceOf(IngenTilgang.class);

        forventLoggetFeil();
    }

    @Test(expected = IngenTilgang.class)
    public void exceptionVedIngenTilgang__denyForBegge() {
        pepOrginalDeny();
        pepSammenligneMedDeny();
        comparator.get(
                () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                () -> veilarbPepClient.sjekkLesetilgang(abacPersonId));

        forventLoggetFeil();
    }


    @Test
    public void booleanForTilgangssjekk_harTilgangTilEnhet() {
        when(pepOrginal.nyRequest()).thenReturn(new RequestData());
        when(pepOrginal.harTilgang(any(RequestData.class))).thenReturn(new BiasedDecisionResponse(Decision.Permit, null));
        when(pepSammenligneMed.nyRequest()).thenReturn(new RequestData());
        when(pepSammenligneMed.harTilgang(any(RequestData.class))).thenReturn(new BiasedDecisionResponse(Decision.Permit, null));
        boolean resultat = comparator.get(
                () -> veilarbAbacPepClient.harTilgangTilEnhet(enhet),
                () -> veilarbPepClient.harTilgangTilEnhet(enhet));

        assertTrue(resultat);
    }


    private void pepOrginalPermit() {
        when(pepOrginal.harInnloggetBrukerTilgangTilPerson(any(), any(), any(), any())).thenReturn(new BiasedDecisionResponse(Decision.Permit, null));
    }

    private void pepOrginalDeny() {
        when(pepOrginal.harInnloggetBrukerTilgangTilPerson(any(), any(), any(), any())).thenReturn(new BiasedDecisionResponse(Decision.Deny, null));
    }

    private void pepOrginalMedFeil() {
        when(pepOrginal.harInnloggetBrukerTilgangTilPerson(any(), any(), any(), any())).thenThrow(new RuntimeException("feil"));
    }

    private void pepSammenligneMedPermit() {
        when(pepSammenligneMed.harInnloggetBrukerTilgangTilPerson(any(), any(), any(), any())).thenReturn(new BiasedDecisionResponse(Decision.Permit, null));
    }

    private void pepSammenligneMedDeny() {
        when(pepSammenligneMed.harInnloggetBrukerTilgangTilPerson(any(), any(), any(), any())).thenReturn(new BiasedDecisionResponse(Decision.Deny, null));
    }

    private void pepSammenligneMedFeil() {
        when(pepSammenligneMed.harInnloggetBrukerTilgangTilPerson(any(), any(), any(), any())).thenThrow(new RuntimeException("feil"));
    }

    private void forventIngenLogging() {
        verify(log, never()).warn(any());
        verify(log, never()).warn(any(), any(Throwable.class));
    }

    private void forventLoggetAvvik(String message) {
        verify(log).warn(message);
        verify(log, never()).warn(any(), any(Throwable.class));
    }

    private void forventLoggetFeil() {
        verify(log).warn(eq("Feil i kall mot pep for sammenligning"), any(Throwable.class));
        verify(log, never()).warn(any());
    }
}
