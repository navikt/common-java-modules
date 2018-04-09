package no.nav.sbl.featuretoggle;

import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.System.setProperty;
import static no.nav.sbl.featuretoggle.SystemPropertyFeatureToggleTest.TestFeature.DEFAULT_AKTIV_FEATURE;
import static no.nav.sbl.featuretoggle.SystemPropertyFeatureToggleTest.TestFeature.DEFAULT_INAKTIV_FEATURE;
import static org.assertj.core.api.Assertions.assertThat;


public class SystemPropertyFeatureToggleTest {

    @After
    public void cleanup() {
        Arrays.stream(TestFeature.values()).forEach(f -> System.clearProperty(f.getSystemVariabelNavn()));
    }

    @Test
    public void erAktiv__folger_default_aktiv_hvis_variabel_er_udefinert() {
        assertThat(DEFAULT_AKTIV_FEATURE.erAktiv()).isTrue();
        assertThat(DEFAULT_INAKTIV_FEATURE.erAktiv()).isFalse();
    }

    @Test
    public void erAktiv__folger_variabel_hvis_definert() {
        setProperty(DEFAULT_AKTIV_FEATURE.getSystemVariabelNavn(), FALSE.toString());
        assertThat(DEFAULT_AKTIV_FEATURE.erAktiv()).isFalse();

        setProperty(DEFAULT_INAKTIV_FEATURE.getSystemVariabelNavn(), TRUE.toString());
        assertThat(DEFAULT_INAKTIV_FEATURE.erAktiv()).isTrue();
    }

    public enum TestFeature implements SystemPropertyFeatureToggle {
        DEFAULT_AKTIV_FEATURE("feature.default.aktiv", true),
        DEFAULT_INAKTIV_FEATURE("feature.default.inaktiv", false);

        private final String systemVariabel;
        private final boolean defaultAktiv;

        TestFeature(String systemVariabel, boolean defaultAktiv) {
            this.systemVariabel = systemVariabel;
            this.defaultAktiv = defaultAktiv;
        }

        @Override
        public String getSystemVariabelNavn() {
            return systemVariabel;
        }

        @Override
        public boolean erDefaultAktiv() {
            return defaultAktiv;
        }

    }


}