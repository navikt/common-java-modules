package no.nav.common.utils;

import org.junit.Before;
import org.junit.Test;

import static java.lang.System.setProperty;
import static no.nav.common.utils.EnvironmentUtils.*;
import static no.nav.common.utils.EnvironmentUtils.Type.PUBLIC;
import static no.nav.common.utils.EnvironmentUtils.Type.SECRET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class EnvironmentUtilsTest {

    private static final String PROPERTY_NAME = EnvironmentUtilsTest.class.getName();

    @Before
    public void setup() {
        System.clearProperty(PROPERTY_NAME);
    }

    @Test
    public void isDevelopment_should_return_true_for_dev_env() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "dev-fss");
        assertTrue(EnvironmentUtils.isDevelopment().get());

        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "dev-sbs");
        assertTrue(EnvironmentUtils.isDevelopment().get());

        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "dev-gcp");
        assertTrue(EnvironmentUtils.isDevelopment().get());
    }

    @Test
    public void isDevelopment_should_return_false_for_not_dev_env() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "prod-fss");
        assertFalse(EnvironmentUtils.isDevelopment().get());
    }

    @Test
    public void isDevelopment_should_return_empty_if_missing() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "");
        assertTrue(EnvironmentUtils.isDevelopment().isEmpty());
    }

    @Test
    public void isProduction_should_check_correct_envs() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "prod-fss");
        assertTrue(EnvironmentUtils.isProduction().get());

        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "prod-sbs");
        assertTrue(EnvironmentUtils.isProduction().get());

        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "prod-gcp");
        assertTrue(EnvironmentUtils.isProduction().get());
    }

    @Test
    public void isProduction_should_return_false_for_not_prod_env() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "dev-fss");
        assertFalse(EnvironmentUtils.isProduction().get());
    }

    @Test
    public void isProduction_should_return_empty_if_missing() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "");
        assertTrue(EnvironmentUtils.isProduction().isEmpty());
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
        System.setProperty("b", "");
        System.setProperty("c", "c");
        System.setProperty("d", "d");

        assertThat(getRequiredProperty("a", "b", "c", "d", "e")).isEqualTo("c");
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
    public void requireNamespace_skal_kaste_exception_hvis_ikke_satt() {
        System.clearProperty(NAIS_NAMESPACE_PROPERTY_NAME);
        assertThat(EnvironmentUtils.getNamespace()).isEmpty();
        assertThatThrownBy(EnvironmentUtils::requireNamespace).hasMessageContaining(NAIS_NAMESPACE_PROPERTY_NAME);
    }

    @Test
    public void getNamespace() {
        System.setProperty(NAIS_NAMESPACE_PROPERTY_NAME, "testnamespace");
        assertThat(EnvironmentUtils.getNamespace()).hasValue("testnamespace");
        assertThat(EnvironmentUtils.requireNamespace()).isEqualTo("testnamespace");
    }

    @Test
    public void resolveHostname() {
        assertThat(EnvironmentUtils.resolveHostName()).isNotEmpty();
    }


}
