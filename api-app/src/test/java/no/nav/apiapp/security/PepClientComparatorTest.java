package no.nav.apiapp.security;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.RequestData;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import org.junit.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PepClientComparatorTest {

    private static final String SYSTEM_TOKEN = "token";
    private final Pep pepOrginal = mock(Pep.class);
    private final Pep pepSammenligneMed = mock(Pep.class);
    private final PepClient veilarbPepClient = new PepClient(pepSammenligneMed, "veilarb", ResourceType.VeilArbPerson);
    private final Logger log = mock(Logger.class);
    private final PepClientComparatorImpl sammenligner = new PepClientComparatorImpl(log);
    private final Bruker bruker = Bruker.fraFnr("fnr").medAktoerId("aktorId");
    private final String fnr = "fnr";
    private final String enhet = "enhet";

    VeilarbAbacPepClient.Builder builder = VeilarbAbacPepClient.ny()
            .medPep(pepOrginal)
            .brukAktoerId(() -> false)
            .medSystemUserTokenProvider(() -> SYSTEM_TOKEN)
            .medVeilarbAbacUrl("test");

    VeilarbAbacPepClient veilarbAbacPepClient = builder.bygg();

    @Test
    public void exceptionVedIngenTilgang__permitForBegge__sjekkLesetilgang() {
        pepPermit(pepOrginal);
        pepPermit(pepSammenligneMed);

        sammenligner.get(
                () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                () -> veilarbPepClient.sjekkLesetilgangTilFnr(fnr));

        forventIngenLogging();
    }

    @Test
    public void exceptionVedIngenTilgang__permitForBegge__sjekkSkrivegang() {
        pepPermit(pepOrginal);
        pepPermit(pepSammenligneMed);

        sammenligner.get(
                () -> veilarbAbacPepClient.sjekkSkrivetilgangTilBruker(bruker),
                () -> veilarbPepClient.sjekkSkrivetilgangTilFnr(fnr));

        forventIngenLogging();
    }

    @Test
    public void exceptionVedIngenTilgang__permitForOrginal__denyForSammenligneMed() {
        pepPermit(pepOrginal);
        pepDeny(pepSammenligneMed);

        sammenligner.get(
                () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                () -> veilarbPepClient.sjekkLesetilgangTilFnr(fnr));

        forventLoggetAvvik("Avvik i resultat fra pep sammenligning. Forventet permit fikk deny.");
    }

    @Test
    public void exceptionVedIngenTilgang__denyForOrginal__permitForSammenligneMed() {
        pepDeny(pepOrginal);
        pepPermit(pepSammenligneMed);

        assertThatThrownBy(() ->
                sammenligner.get(
                        () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                        () -> veilarbPepClient.sjekkLesetilgangTilFnr(fnr)))
                .isInstanceOf(IngenTilgang.class);

        forventLoggetAvvik("Avvik i resultat fra pep sammenligning. Forventet deny fikk permit.");
    }

    @Test
    public void exceptionVedIngenTilgang__permitForOrginal__feilForSammenligneMed() {
        pepPermit(pepOrginal);
        pepFeil(pepSammenligneMed);

        sammenligner.get(
                () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                () -> veilarbPepClient.sjekkLesetilgangTilFnr(fnr));

        forventLoggetFeil();
    }

    @Test
    public void exceptionVedIngenTilgang__denyForOrginal__feilForSammenligneMed() {
        pepDeny(pepOrginal);
        pepFeil(pepSammenligneMed);

        assertThatThrownBy(() ->
                sammenligner.get(
                        () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                        () -> veilarbPepClient.sjekkLesetilgangTilFnr(fnr)))
                .isInstanceOf(IngenTilgang.class);

        forventLoggetFeil();
    }

    @Test
    public void exceptionVedIngenTilgang__feilForOrginal__permitForSammenligneMed() {
        pepFeil(pepOrginal);
        pepPermit(pepSammenligneMed);

        assertThatThrownBy(() ->
                sammenligner.get(
                        () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                        () -> veilarbPepClient.sjekkLesetilgangTilFnr(fnr)))
                .isInstanceOf(IngenTilgang.class);

        forventLoggetAvvik("Avvik i resultat fra pep sammenligning. Forventet deny fikk permit.");
    }

    @Test
    public void exceptionVedIngenTilgang__feilForOrginal__denyForSammenligneMed() {
        pepFeil(pepOrginal);
        pepDeny(pepSammenligneMed);

        assertThatThrownBy(() ->
                sammenligner.get(
                        () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                        () -> veilarbPepClient.sjekkLesetilgangTilFnr(fnr)))
                .isInstanceOf(IngenTilgang.class);

        forventIngenLogging();
    }

    @Test
    public void exceptionVedIngenTilgang__feilForOrginal__feilForSammenligneMed() {
        pepFeil(pepOrginal);
        pepFeil(pepSammenligneMed);

        assertThatThrownBy(() ->
                sammenligner.get(
                        () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                        () -> veilarbPepClient.sjekkLesetilgangTilFnr(fnr)))
                .isInstanceOf(IngenTilgang.class);

        forventLoggetFeil();
    }

    @Test
    public void exceptionVedIngenTilgang__denyForBegge() {
        pepDeny(pepOrginal);
        pepDeny(pepSammenligneMed);

        assertThatThrownBy(() ->
        sammenligner.get(
                () -> veilarbAbacPepClient.sjekkLesetilgangTilBruker(bruker),
                () -> veilarbPepClient.sjekkLesetilgangTilFnr(fnr))).isInstanceOf(IngenTilgang.class);

        forventIngenLogging();
    }

    @Test
    public void booleanForTilgangssjekk__permitForBegge__harTilgangTilEnhet() {
        pepPermit(pepOrginal);
        pepPermit(pepSammenligneMed);

        boolean resultat = sammenligner.get(
                () -> veilarbAbacPepClient.harTilgangTilEnhet(enhet),
                () -> veilarbPepClient.harTilgangTilEnhet(enhet));

        assertTrue(resultat);

        forventIngenLogging();
    }

    @Test
    public void booleanForTilgangssjekk__permitForOrginal__denyForSammenligneMed() {
        pepPermit(pepOrginal);
        pepDeny(pepSammenligneMed);

        boolean resultat = sammenligner.get(
                () -> veilarbAbacPepClient.harTilgangTilEnhet(enhet),
                () -> veilarbPepClient.harTilgangTilEnhet(enhet));

        assertTrue(resultat);

        forventLoggetAvvik("Avvik i resultat fra pep sammenligning. Forventet permit fikk deny.");
    }

    @Test
    public void booleanForTilgangssjekk__denyForOrginal__permitForSammenligneMed() {
        pepDeny(pepOrginal);
        pepPermit(pepSammenligneMed);

        boolean resultat = sammenligner.get(
                () -> veilarbAbacPepClient.harTilgangTilEnhet(enhet),
                () -> veilarbPepClient.harTilgangTilEnhet(enhet));

        assertFalse(resultat);

        forventLoggetAvvik("Avvik i resultat fra pep sammenligning. Forventet deny fikk permit.");
    }

    @Test
    public void booleanForTilgangssjekk__permitForOrginal__feilForSammenligneMed() {
        pepPermit(pepOrginal);
        pepFeil(pepSammenligneMed);

        boolean resultat = sammenligner.get(
                () -> veilarbAbacPepClient.harTilgangTilEnhet(enhet),
                () -> veilarbPepClient.harTilgangTilEnhet(enhet));

        assertTrue(resultat);

        forventLoggetFeil();
    }

    @Test
    public void booleanForTilgangssjekk__denyForOrginal__feilForSammenligneMed() {
        pepDeny(pepOrginal);
        pepFeil(pepSammenligneMed);

        boolean resultat = sammenligner.get(
                () -> veilarbAbacPepClient.harTilgangTilEnhet(enhet),
                () -> veilarbPepClient.harTilgangTilEnhet(enhet));

        assertFalse(resultat);

        forventLoggetFeil();
    }

    @Test
    public void booleanForTilgangssjekk__feilForOrginal__permitForSammenligneMed() {
        pepFeil(pepOrginal);
        pepPermit(pepSammenligneMed);

        boolean resultat = sammenligner.get(
                () -> veilarbAbacPepClient.harTilgangTilEnhet(enhet),
                () -> veilarbPepClient.harTilgangTilEnhet(enhet));

        assertFalse(resultat);

        forventLoggetAvvik("Avvik i resultat fra pep sammenligning. Forventet deny fikk permit.");
    }

    @Test
    public void booleanForTilgangssjekk__feilForOrginal__denyForSammenligneMed() {
        pepFeil(pepOrginal);
        pepDeny(pepSammenligneMed);

        boolean resultat = sammenligner.get(
                () -> veilarbAbacPepClient.harTilgangTilEnhet(enhet),
                () -> veilarbPepClient.harTilgangTilEnhet(enhet));

        assertFalse(resultat);

        forventIngenLogging();
    }

    @Test
    public void booleanForTilgangssjekk__feilForOrginal__feilForSammenligneMed() {
        pepFeil(pepOrginal);
        pepFeil(pepSammenligneMed);

        boolean resultat = sammenligner.get(
                () -> veilarbAbacPepClient.harTilgangTilEnhet(enhet),
                () -> veilarbPepClient.harTilgangTilEnhet(enhet));

        assertFalse(resultat);

        forventLoggetFeil();
    }

    @Test
    public void booleanForTilgangssjekk__denyForBegge() {
        pepDeny(pepOrginal);
        pepDeny(pepSammenligneMed);

        boolean resultat = sammenligner.get(
                () -> veilarbAbacPepClient.harTilgangTilEnhet(enhet),
                () -> veilarbPepClient.harTilgangTilEnhet(enhet));

        assertFalse(resultat);

        forventIngenLogging();
    }

    @Test
    public void veilarbPepClient__booleanForTilgangssjekk__kasterExceptionVedFeil() {
        Pep pep1 = mock(Pep.class);
        PepClient client1 = new PepClient(pep1, "veilarb", ResourceType.VeilArbPerson);
        Pep pep2 = mock(Pep.class);
        PepClient client2 = new PepClient(pep2, "veilarb", ResourceType.VeilArbPerson);

        pepFeil(pep1);
        pepPermit(pep2);

        assertThatThrownBy(() ->
                sammenligner.get(
                        () -> client1.harTilgangTilEnhet(enhet),
                        () -> client2.harTilgangTilEnhet(enhet))
        ).isInstanceOf(RuntimeException.class);

        forventIngenLogging();
    }

    @Test
    public void veilarbPepClient__exceptionVedIngenTilgang__kasterExceptionVedFeil() {
        Pep pep1 = mock(Pep.class);
        PepClient client1 = new PepClient(pep1, "veilarb", ResourceType.VeilArbPerson);
        Pep pep2 = mock(Pep.class);
        PepClient client2 = new PepClient(pep2, "veilarb", ResourceType.VeilArbPerson);

        pepFeil(pep1);
        pepPermit(pep2);

        assertThatThrownBy(() ->
                sammenligner.get(
                        () -> client1.sjekkSkrivetilgangTilFnr(fnr),
                        () -> client2.sjekkSkrivetilgangTilAktorId("aktorId"))
        ).isInstanceOf(RuntimeException.class);

        forventIngenLogging();
    }


    private void pepPermit(Pep pep) {
        when(pep.harInnloggetBrukerTilgangTilPerson(any(), any(), any(), any(), any())).thenReturn(new BiasedDecisionResponse(Decision.Permit, null));
        when(pep.nyRequest()).thenReturn(new RequestData());
        when(pep.harTilgang(any(RequestData.class), any())).thenReturn(new BiasedDecisionResponse(Decision.Permit, null));
    }

    private void pepDeny(Pep pep) {
        when(pep.harInnloggetBrukerTilgangTilPerson(any(), any(), any(), any(), any())).thenReturn(new BiasedDecisionResponse(Decision.Deny, null));
        when(pep.nyRequest()).thenReturn(new RequestData());
        when(pep.harTilgang(any(RequestData.class), any())).thenReturn(new BiasedDecisionResponse(Decision.Deny, null));
    }

    private void pepFeil(Pep pep) {
        when(pep.harInnloggetBrukerTilgangTilPerson(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("feil"));
        when(pep.nyRequest()).thenReturn(new RequestData());
        when(pep.harTilgang(any(RequestData.class), any())).thenThrow(new RuntimeException("feil"));
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
