package no.nav.apiapp.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static java.lang.System.setProperty;
import static no.nav.apiapp.util.PropertyUtils.getOptionalObjectProperty;
import static no.nav.apiapp.util.PropertyUtils.getRequiredProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class PropertyUtilsTest {

    private static final String PROPERTY_NAME = PropertyUtilsTest.class.getName();

    @Before
    public void setup() {
        Properties systemProperties = System.getProperties();
        systemProperties.remove(PROPERTY_NAME);
        systemProperties.remove(PropertyUtilsTest.class);
    }

    @Test
    public void getRequiredProperty_finnerVerdi() {
        String value = "verdi";
        setProperty(PROPERTY_NAME, value);
        assertThat(getRequiredProperty(PROPERTY_NAME)).isEqualTo(value);
    }

    @Test
    public void getRequiredProperty_manglerProperty_kasterFeil() {
        assertThatThrownBy(() -> getRequiredProperty(PROPERTY_NAME))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith(PROPERTY_NAME);
    }

    @Test
    public void getOptionalObjectProperty_kanHenteObjekterFraSystemProperties() {
        assertThat(getOptionalObjectProperty(PropertyUtilsTest.class)).isEmpty();
        System.getProperties().put(PropertyUtilsTest.class, this);
        assertThat(getOptionalObjectProperty(PropertyUtilsTest.class)).hasValue(this);
    }

    @Test
    public void getOptionalObjectProperty_kanHenteObjekterFraSystemProperties2() {
        assertThat(getOptionalObjectProperty(PropertyUtilsTest.class)).isEmpty();
        System.getProperties().put(PropertyUtilsTest.class.getName(), this);
        assertThat(getOptionalObjectProperty(PropertyUtilsTest.class)).hasValue(this);
    }

}