package no.nav.sbl.util;

import org.junit.Before;
import org.junit.Test;

import static java.lang.System.setProperty;
import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static no.nav.sbl.util.EnvironmentUtils.*;
import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.*;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;
import static no.nav.sbl.util.PropertyUtils.getOptionalProperty;
import static no.nav.sbl.util.PropertyUtils.getRequiredProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class EnvironmentUtilsTest {

    private static final String PROPERTY_NAME = EnvironmentUtilsTest.class.getName();

    @Before
    public void setup() {
        System.clearProperty(PROPERTY_NAME);
    }

    @Test
    public void getRequiredProperty__finner_verdi() {
        String value = "verdi";
        setProperty(PROPERTY_NAME, value);
        assertThat(getRequiredProperty(PROPERTY_NAME)).isEqualTo(value);
    }

    @Test
    public void getRequiredProperty__mangler_property__kasterFeil() {
        assertThatThrownBy(() -> getRequiredProperty(PROPERTY_NAME))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith(PROPERTY_NAME);
    }

    @Test
    public void getRequiredProperty__finner_verdi_ogsa_i_environment() {
        assertThat(getRequiredProperty("PATH")).isNotNull();
    }

    @Test
    public void getRequiredProperty__sjekk_alle_properties_i_rekkefolge() {
        setTemporaryProperty("b", "", () -> {
            setTemporaryProperty("c", "c", () -> { // dette er den vi vil ha!
                setTemporaryProperty("d", "d", () -> {
                    assertThat(getRequiredProperty("a", "b", "c", "d", "e"))
                            .isEqualTo("c");
                });
            });
        });
    }

    @Test
    public void getEnvironmentClass__default_ukjent() {
        System.clearProperty(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME);
        assertThat(getEnvironmentClass()).isEqualTo(UKNOWN);
    }

    @Test
    public void getEnvironmentClass__gjenkjenner_miljo() {
        assertGetEnvironmentClass("t", T);
        assertGetEnvironmentClass("q", Q);
        assertGetEnvironmentClass("p", P);
    }

    @Test
    public void getOptionalProperty__() {
        String value = "verdi";
        setProperty(PROPERTY_NAME, value);
        assertThat(getOptionalProperty(PROPERTY_NAME)).hasValue(value);
        assertThat(getOptionalProperty("asdfasdfasdf")).isEmpty();
    }

    @Test
    public void getOptionalProperty__tom_streng_regnes_som_fraverende() {
        setProperty(PROPERTY_NAME, "   \n   \t  ");
        assertThat(getOptionalProperty(PROPERTY_NAME)).isEmpty();
    }

    @Test
    public void setProperty__logges() {
        String value = "123";
        EnvironmentUtils.setProperty(PROPERTY_NAME, value, PUBLIC);
        EnvironmentUtils.setProperty(PROPERTY_NAME, value, SECRET);
        assertThat(getOptionalProperty(PROPERTY_NAME)).hasValue(value);
    }

    @Test
    public void getRequiredProperty_leser_ogsa_other_properties_fra_environment() {
        String requiredProperty = EnvironmentUtils.getRequiredProperty("SOMETHING_SOMETHING", "PATH");
        assertThat(requiredProperty).isNotBlank();
    }


    @Test
    public void getApplicationVersion_from_environment() {
        setTemporaryProperty(APP_VERSION_PROPERTY_NAME, "123", () -> {
            assertThat(EnvironmentUtils.getApplicationVersion()).hasValue("123");
        });
    }

    @Test
    public void getApplicationName() {
        assertThat(EnvironmentUtils.getApplicationName()).isEmpty();
        assertThatThrownBy(EnvironmentUtils::requireApplicationName).hasMessageContaining(APP_NAME_PROPERTY_NAME);

        setTemporaryProperty(APP_NAME_PROPERTY_NAME, "testapp", () -> {
            assertThat(EnvironmentUtils.getApplicationName()).hasValue("testapp");
            assertThat(EnvironmentUtils.requireApplicationName()).isEqualTo("testapp");
        });
    }

    @Test
    public void getEnvironmentName() {
        assertThat(EnvironmentUtils.getEnvironmentName()).isEmpty();
        assertThatThrownBy(EnvironmentUtils::requireEnvironmentName).hasMessageContaining(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME);

        setTemporaryProperty(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME, "q42", () -> {
            assertThat(EnvironmentUtils.getEnvironmentName()).hasValue("q42");
            assertThat(EnvironmentUtils.requireEnvironmentName()).isEqualTo("q42");
        });
    }

    @Test
    public void resolveHostname() {
        assertThat(EnvironmentUtils.resolveHostName()).isNotEmpty();
    }

    private void assertGetEnvironmentClass(String verdi, EnvironmentUtils.EnviromentClass enviromentClass) {
        setTemporaryProperty(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME, verdi, () -> {
            assertThat(getEnvironmentClass()).isEqualTo(enviromentClass);
            assertThat(isEnvironmentClass(enviromentClass)).isTrue();
        });
    }

}
