package no.nav.apiapp.security;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.brukerdialog.security.context.SubjectExtension;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestUser;
import no.nav.dialogarena.config.security.ISSOProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static no.nav.apiapp.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("unused")
@ExtendWith(SubjectExtension.class)
public interface PepClientTester {

    PepClient getPepClient();

    default void setVeilederFraFasitAlias(String fasitAlias, SubjectExtension.SubjectStore subjectExtension) {
        TestUser veileder = FasitUtils.getTestUser(fasitAlias);
        subjectExtension.setSubject(new Subject(
                veileder.getUsername(),
                IdentType.InternBruker,
                SsoToken.oidcToken(ISSOProvider.getISSOToken(veileder))
        ));
    }

    default String hentFnrFraFasit() {
        return FasitUtils.getTestUser(BRUKER_UNDER_OPPFOLGING_ALIAS).getUsername();
    }

    @Test
    default void sjekkTilgangTilFnr_veilederHarTilgang(SubjectExtension.SubjectStore subjectExtension) {
        setVeilederFraFasitAlias(PRIVELIGERT_VEILEDER_ALIAS, subjectExtension);
        PepClient pepClient = getPepClient();
        pepClient.sjekkLeseTilgangTilFnr(hentFnrFraFasit());
    }

    @Test
    default void sjekkTilgangTilFnr_veilederHarIkkeTilgang(SubjectExtension.SubjectStore subjectExtension) {
        setVeilederFraFasitAlias(LITE_PRIVELIGERT_VEILEDER_ALIAS, subjectExtension);
        PepClient pepClient = getPepClient();
        assertThatThrownBy(() -> pepClient.sjekkLeseTilgangTilFnr(hentFnrFraFasit())).isExactlyInstanceOf(IngenTilgang.class);
    }

    @Test
    @SneakyThrows
    default void harTilgangTilEnhet_veilederHarTilgang(SubjectExtension.SubjectStore subjectExtension) {
        setVeilederFraFasitAlias(PRIVELIGERT_VEILEDER_ALIAS, subjectExtension);
        String sentralEnhet = FasitUtils.getTestDataProperty(SENTRAL_ENHET_ALIAS).orElseThrow(IllegalStateException::new);
        PepClient pepClient = getPepClient();
        assertThat(pepClient.harTilgangTilEnhet(sentralEnhet)).isTrue();
    }

    @Test
    @SneakyThrows
    default void harTilgangTilEnhet_veilederHarIkkeTilgang(SubjectExtension.SubjectStore subjectExtension) {
        setVeilederFraFasitAlias(LITE_PRIVELIGERT_VEILEDER_ALIAS, subjectExtension);
        String sentralEnhet = FasitUtils.getTestDataProperty(SENTRAL_ENHET_ALIAS).orElseThrow(IllegalStateException::new);
        PepClient pepClient = getPepClient();
        assertThat(pepClient.harTilgangTilEnhet(sentralEnhet)).isFalse();
    }

    @Test
    @SneakyThrows
    default void harTilgangTilEnhet__mangler_enhet__veilederHarIkkeTilgang(SubjectExtension.SubjectStore subjectExtension) {
        setVeilederFraFasitAlias(PRIVELIGERT_VEILEDER_ALIAS, subjectExtension);
        PepClient pepClient = getPepClient();
        assertThat(pepClient.harTilgangTilEnhet(null)).isFalse();
        assertThat(pepClient.harTilgangTilEnhet("")).isFalse();
    }

}
