package no.nav.apiapp.security;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.brukerdialog.security.context.SubjectHandlerUtils;
import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.dialogarena.config.security.ISSOProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static no.nav.apiapp.TestData.*;
import static no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY;
import static no.nav.brukerdialog.security.context.SubjectHandlerUtils.setSubject;
import static no.nav.dialogarena.config.util.Util.setProperty;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("unused")
public interface PepClientTester {

    PepClient getPepClient();

    @BeforeEach
    default void setupPepClientTest() {
        setProperty(SUBJECTHANDLER_KEY, ThreadLocalSubjectHandler.class.getName());
        setSubject(new SubjectHandlerUtils.SubjectBuilder(KJENT_VEILEDER_IDENT, IdentType.InternBruker, ISSOProvider.getISSOToken()).getSubject());
    }

    @Test
    default void sjekkTilgangTilFnr_veilederHarTilgang() {
        getPepClient().sjekkTilgangTilFnr(KJENT_IDENT_FOR_KJENT_VEILEDER);
    }

    @Test
    default void sjekkTilgangTilFnr_veilederHarIkkeTilgang() {
        assertThatThrownBy(() -> getPepClient().sjekkTilgangTilFnr(KJENT_IDENT)).isExactlyInstanceOf(IngenTilgang.class);
    }

}
