package no.nav.common.utils;

import org.junit.Before;
import org.junit.Test;

import static java.lang.System.setProperty;
import static no.nav.common.test.SystemProperties.setTemporaryProperty;
import static no.nav.common.utils.EnvironmentUtils.*;
import static no.nav.common.utils.EnvironmentUtils.Type.PUBLIC;
import static no.nav.common.utils.EnvironmentUtils.Type.SECRET;
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
    public void getNamespace() {
        assertThat(EnvironmentUtils.getNamespace()).isEmpty();
        assertThatThrownBy(EnvironmentUtils::requireNamespace).hasMessageContaining(NAIS_NAMESPACE_PROPERTY_NAME);

        setTemporaryProperty(NAIS_NAMESPACE_PROPERTY_NAME, "testnamespace", () -> {
            assertThat(EnvironmentUtils.getNamespace()).hasValue("testnamespace");
            assertThat(EnvironmentUtils.requireNamespace()).isEqualTo("testnamespace");
        });
    }

    @Test
    public void resolveHostname() {
        assertThat(EnvironmentUtils.resolveHostName()).isNotEmpty();
    }


}
