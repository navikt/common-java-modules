package no.nav.common.test.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.Statement;

public class SystemPropertiesRuleTest {
    private static final String PROPERTY = "common.test.system-properties-rule";
    private static final String ADDED_PROPERTY = PROPERTY + ".added";
    private Properties originalProperties;

    @Before
    public void before() {
        originalProperties = (Properties) System.getProperties().clone();
    }

    @After
    public void after() {
        System.setProperties(originalProperties);
    }

    @Test
    public void gjenoppretter_tilstanden_fra_foer_hver_evaluering() throws Throwable {
        // Arrange
        SystemPropertiesRule rule = new SystemPropertiesRule();
        System.setProperty(PROPERTY, "first");
        System.clearProperty(ADDED_PROPERTY);
        Statement evaluation = rule.apply(
                statement(() -> {
                    System.clearProperty(PROPERTY);
                    System.setProperty(ADDED_PROPERTY, "added");
                }),
                null,
                this);

        // Act
        evaluation.evaluate();
        Properties firstResult = (Properties) System.getProperties().clone();
        System.setProperty(PROPERTY, "second");
        evaluation.evaluate();

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
        Statement evaluation = rule.apply(
                statement(() -> {
                    System.setProperties(new Properties());
                    throw failure;
                }),
                null,
                this);

        // Act
        Throwable result = catchThrowable(evaluation::evaluate);

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
        Statement evaluation =
                rule.apply(statement(() -> duringEvaluation[0] = System.getProperty(PROPERTY)), null, this);

        // Act
        evaluation.evaluate();

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
        Statement evaluation = rule.apply(
                statement(() -> {
                    returnedRule[0] = rule.setProperty(PROPERTY, "configured");
                    duringEvaluation[0] = System.getProperty(PROPERTY);
                }),
                null,
                this);

        // Act
        evaluation.evaluate();
        rule.apply(statement(() -> nextEvaluation[0] = System.getProperty(PROPERTY)), null, this)
                .evaluate();

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
        Statement evaluation =
                rule.apply(statement(() -> duringEvaluation[0] = System.getProperty(PROPERTY)), null, this);

        // Act
        evaluation.evaluate();

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
        Statement evaluation = rule.apply(
                statement(() -> {
                    duringEvaluation[0] = System.getProperty(PROPERTY);
                    throw failure;
                }),
                null,
                this);

        // Act
        Throwable result = catchThrowable(evaluation::evaluate);

        // Assert
        assertThat(result).isSameAs(failure);
        assertThat(duringEvaluation[0]).isEqualTo("configured");
        assertThat(System.getProperty(PROPERTY)).isEqualTo("original");
    }

    private static Statement statement(ThrowingAction action) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                action.run();
            }
        };
    }

    private interface ThrowingAction {
        void run() throws Throwable;
    }
}
