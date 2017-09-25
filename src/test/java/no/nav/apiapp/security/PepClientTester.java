package no.nav.apiapp.security;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.brukerdialog.security.context.SubjectHandlerUtils;
import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestUser;
import no.nav.dialogarena.config.security.ISSOProvider;
import org.junit.jupiter.api.AfterEach;
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
    }

    @AfterEach
    default void cleanupPepClientTest() {
        setSubject(null);
    }

    default void setVeilederFraFasitAlias(String fasitAlias) {
        TestUser veileder = FasitUtils.getTestUser(fasitAlias);
        setSubject(new SubjectHandlerUtils.SubjectBuilder(
                veileder.getUsername(),
                IdentType.InternBruker,
                ISSOProvider.getISSOToken(veileder)
        ).getSubject());
    }

    default String hentFnrFraFasit() {
        return FasitUtils.getTestUser(BRUKER_UNDER_OPPFOLGING_ALIAS).getUsername();
    }

    @Test
    default void sjekkTilgangTilFnr_veilederHarTilgang() {
        setVeilederFraFasitAlias(PRIVELIGERT_VEILEDER_ALIAS);
        PepClient pepClient = getPepClient();
        pepClient.sjekkLeseTilgangTilFnr(hentFnrFraFasit());
    }

    @Test
    default void sjekkTilgangTilFnr_veilederHarIkkeTilgang() {
        setVeilederFraFasitAlias(LITE_PRIVELIGERT_VEILEDER_ALIAS);
        PepClient pepClient = getPepClient();
        assertThatThrownBy(() -> pepClient.sjekkLeseTilgangTilFnr(hentFnrFraFasit())).isExactlyInstanceOf(IngenTilgang.class);
    }

}
