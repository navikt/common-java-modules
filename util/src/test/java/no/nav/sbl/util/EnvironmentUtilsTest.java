package no.nav.sbl.util;

import org.junit.Before;
import org.junit.Test;

import static java.lang.System.setProperty;
import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static no.nav.sbl.util.EnvironmentUtils.ENVIRONMENT_CLASS_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.*;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;
import static no.nav.sbl.util.EnvironmentUtils.getEnvironmentClass;
import static no.nav.sbl.util.EnvironmentUtils.isEnvironmentClass;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;
import static no.nav.sbl.util.PropertyUtils.getOptionalProperty;
import static no.nav.sbl.util.PropertyUtils.getRequiredProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class EnvironmentUtilsTest {

    private static final String PROPERTY_NAME = EnvironmentUtilsTest.class.getName();
    public static final String MILJO_PROPERTY_NAME = "environment.class";

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
        System.getenv().keySet().forEach(k -> assertThat(getRequiredProperty(k)).isNotEmpty());
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
        System.clearProperty(ENVIRONMENT_CLASS_PROPERTY_NAME);
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
        setProperty(PROPERTY_NAME, value, PUBLIC);
        setProperty(PROPERTY_NAME, value, SECRET);
        assertThat(getOptionalProperty(PROPERTY_NAME)).hasValue(value);
    }

    @Test
    public void getRequiredProperty_leser_ogsa_other_properties_fra_environment() {
        String requiredProperty = EnvironmentUtils.getRequiredProperty("SOMETHING_SOMETHING", "PATH");
        assertThat(requiredProperty).isNotBlank();
    }

    private void assertGetEnvironmentClass(String verdi, EnvironmentUtils.EnviromentClass enviromentClass) {
        System.setProperty(ENVIRONMENT_CLASS_PROPERTY_NAME, verdi);
        assertThat(getEnvironmentClass()).isEqualTo(enviromentClass);
        assertThat(isEnvironmentClass(enviromentClass)).isTrue();
    }

}
