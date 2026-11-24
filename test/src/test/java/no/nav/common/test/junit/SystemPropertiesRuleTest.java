package no.nav.common.test.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class SystemPropertiesRuleTest {
    private static final String PROPERTY = "common.test.system-properties-rule";
    private static final String ADDED_PROPERTY = PROPERTY + ".added";
    private Properties originalProperties;

    @BeforeEach
    public void before() {
        originalProperties = (Properties) System.getProperties().clone();
    }

    @AfterEach
    public void after() {
        System.setProperties(originalProperties);
    }

    @Test
    public void gjenoppretter_tilstanden_fra_foer_hver_evaluering() throws Throwable {
        // Arrange
        SystemPropertiesRule rule = new SystemPropertiesRule();
        System.setProperty(PROPERTY, "first");
        System.clearProperty(ADDED_PROPERTY);
        Executable evaluation = () -> rule.evaluate(() -> {
            System.clearProperty(PROPERTY);
            System.setProperty(ADDED_PROPERTY, "added");
        });

        // Act
        evaluation.execute();
        Properties firstResult = (Properties) System.getProperties().clone();
        System.setProperty(PROPERTY, "second");
        evaluation.execute();

        // Assert
        assertThat(firstResult.getProperty(PROPERTY)).isEqualTo("first");
        assertThat(firstResult).doesNotContainKey(ADDED_PROPERTY);
        assertThat(System.getProperty(PROPERTY)).isEqualTo("second");
        assertThat(System.getProperties()).doesNotContainKey(ADDED_PROPERTY);
    }

    @Test
    public void gjenoppretter_egenskaper_selv_naar_evalueringen_feiler() {
        // Arrange
        System.setProperty(PROPERTY, "original");
        AssertionError failure = new AssertionError("expected failure");
        SystemPropertiesRule rule = new SystemPropertiesRule();
        Executable evaluation = () -> rule.evaluate(() -> {
            System.setProperties(new Properties());
            throw failure;
        });

        // Act
        Throwable result = catchThrowable(evaluation::execute);

        // Assert
        assertThat(result).isSameAs(failure);
        assertThat(System.getProperty(PROPERTY)).isEqualTo("original");
    }

    @Test
    public void bruker_konfigurerte_egenskaper_kun_under_evalueringen() throws Throwable {
        // Arrange
        System.setProperty(PROPERTY, "original");
        SystemPropertiesRule rule = new SystemPropertiesRule().setProperty(PROPERTY, "configured");
        String beforeEvaluation = System.getProperty(PROPERTY);
        String[] duringEvaluation = new String[1];
        Executable evaluation = () -> rule.evaluate(() -> duringEvaluation[0] = System.getProperty(PROPERTY));

        // Act
        evaluation.execute();

        // Assert
        assertThat(beforeEvaluation).isEqualTo("original");
        assertThat(duringEvaluation[0]).isEqualTo("configured");
        assertThat(System.getProperty(PROPERTY)).isEqualTo("original");
    }

    @Test
    public void setter_egenskaper_umiddelbart_under_evalueringen() throws Throwable {
        // Arrange
        System.setProperty(PROPERTY, "original");
        SystemPropertiesRule rule = new SystemPropertiesRule();
        String[] duringEvaluation = new String[1];
        String[] nextEvaluation = new String[1];
        SystemPropertiesRule[] returnedRule = new SystemPropertiesRule[1];
        Executable evaluation = () -> rule.evaluate(() -> {
            returnedRule[0] = rule.setProperty(PROPERTY, "configured");
            duringEvaluation[0] = System.getProperty(PROPERTY);
        });

        // Act
        evaluation.execute();
        rule.evaluate(() -> nextEvaluation[0] = System.getProperty(PROPERTY));

        // Assert
        assertThat(returnedRule[0]).isSameAs(rule);
        assertThat(duringEvaluation[0]).isEqualTo("configured");
        assertThat(nextEvaluation[0]).isEqualTo("original");
        assertThat(System.getProperty(PROPERTY)).isEqualTo("original");
    }

    @Test
    public void fjerner_konfigurert_egenskap_som_ikke_fantes_foer_evalueringen() throws Throwable {
        // Arrange
        System.clearProperty(PROPERTY);
        SystemPropertiesRule rule = new SystemPropertiesRule().setProperty(PROPERTY, "configured");
        String[] duringEvaluation = new String[1];
        Executable evaluation = () -> rule.evaluate(() -> duringEvaluation[0] = System.getProperty(PROPERTY));

        // Act
        evaluation.execute();

        // Assert
        assertThat(duringEvaluation[0]).isEqualTo("configured");
        assertThat(System.getProperties()).doesNotContainKey(PROPERTY);
    }

    @Test
    public void gjenoppretter_konfigurerte_egenskaper_etter_feil() {
        // Arrange
        System.setProperty(PROPERTY, "original");
        AssertionError failure = new AssertionError("expected failure");
        SystemPropertiesRule rule = new SystemPropertiesRule().setProperty(PROPERTY, "configured");
        String[] duringEvaluation = new String[1];
        Executable evaluation = () -> rule.evaluate(() -> {
            duringEvaluation[0] = System.getProperty(PROPERTY);
            throw failure;
        });

        // Act
        Throwable result = catchThrowable(evaluation::execute);

        // Assert
        assertThat(result).isSameAs(failure);
        assertThat(duringEvaluation[0]).isEqualTo("configured");
        assertThat(System.getProperty(PROPERTY)).isEqualTo("original");
    }
}
