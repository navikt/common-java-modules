package no.nav.common.utils;

import static java.lang.System.setProperty;
import static no.nav.common.utils.EnvironmentUtils.NAIS_CLUSTER_NAME_PROPERTY_NAME;
import static no.nav.common.utils.EnvironmentUtils.NAIS_NAMESPACE_PROPERTY_NAME;
import static no.nav.common.utils.EnvironmentUtils.Type.PUBLIC;
import static no.nav.common.utils.EnvironmentUtils.Type.SECRET;
import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EnvironmentUtilsTest {

    private static final String PROPERTY_NAME = EnvironmentUtilsTest.class.getName();
    private Properties originalProperties;

    @Before
    public void setup() {
        originalProperties = (Properties) System.getProperties().clone();
        System.clearProperty(PROPERTY_NAME);
        for (String property : new String[] {"a", "b", "c", "d", "e", "asdfasdfasdf", "SOMETHING_SOMETHING"}) {
            System.clearProperty(property);
        }
    }

    @After
    public void tearDown() {
        System.setProperties(originalProperties);
    }

    @Test
    public void isDevelopment_should_return_true_for_dev_env() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "dev-fss");
        assertThat(EnvironmentUtils.isDevelopment()).hasValue(true);

        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "dev-sbs");
        assertThat(EnvironmentUtils.isDevelopment()).hasValue(true);

        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "dev-gcp");
        assertThat(EnvironmentUtils.isDevelopment()).hasValue(true);
    }

    @Test
    public void isDevelopment_should_return_false_for_not_dev_env() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "prod-fss");
        assertThat(EnvironmentUtils.isDevelopment()).hasValue(false);
    }

    @Test
    public void isDevelopment_should_return_empty_if_missing() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "");
        assertThat(EnvironmentUtils.isDevelopment()).isEmpty();
    }

    @Test
    public void isProduction_should_check_correct_envs() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "prod-fss");
        assertThat(EnvironmentUtils.isProduction()).hasValue(true);

        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "prod-sbs");
        assertThat(EnvironmentUtils.isProduction()).hasValue(true);

        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "prod-gcp");
        assertThat(EnvironmentUtils.isProduction()).hasValue(true);
    }

    @Test
    public void isProduction_should_return_false_for_not_prod_env() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "dev-fss");
        assertThat(EnvironmentUtils.isProduction()).hasValue(false);
    }

    @Test
    public void isProduction_should_return_empty_if_missing() {
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "");
        assertThat(EnvironmentUtils.isProduction()).isEmpty();
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
