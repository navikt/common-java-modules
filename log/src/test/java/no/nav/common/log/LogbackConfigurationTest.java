package no.nav.common.log;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.After;
import org.junit.Test;

public class LogbackConfigurationTest {

    private static final List<String> CXF_LOGGERE = List.of(
            "org.apache.cxf",
            "org.apache.cxf.services.TestService",
            "no.nav.common.cxf",
            "no.nav.common.cxf.LogInterceptor",
            "prefix.org.apache.cxf.suffix",
            "prefix.no.nav.common.cxf.suffix");
    private final LoggerContext context = new LoggerContext();

    @After
    public void stoppLoggerkontekst() {
        context.stop();
    }

    @Test
    public void stdoutKonfigurasjonSkalStarteUtenAdvarslerEllerFeil() throws Exception {
        // Arrange
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        var configUrl = requireNonNull(getClass().getResource("/logback-test.xml"));

        // Act
        configurator.doConfigure(configUrl);

        // Assert
        assertThat(stdoutAppender().isStarted()).isTrue();
        assertThat(stdoutAppender().getCopyOfAttachedFiltersList())
                .singleElement()
                .satisfies(filter -> assertThat(filter.isStarted()).isTrue());
        assertThat(context.getStatusManager().getCopyOfStatusList())
                .filteredOn(status -> status.getEffectiveLevel() >= Status.WARN)
                .isEmpty();
    }

    @Test
    public void cxfMeldingerUtenomErrorSkalAvvisesFraStdout() throws Exception {
        // Arrange
        lastStdoutKonfigurasjon();

        for (String logger : CXF_LOGGERE) {
            for (Level level : List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.OFF)) {
                // Act
                FilterReply beslutning = filterbeslutning(logger, level);

                // Assert
                assertThat(beslutning).as("%s med nivaa %s", logger, level).isEqualTo(FilterReply.DENY);
            }
        }
    }

    @Test
    public void cxfFeilSkalSlippeGjennomTilMaskering() throws Exception {
        // Arrange
        lastStdoutKonfigurasjon();

        for (String logger : CXF_LOGGERE) {
            // Act
            FilterReply beslutning = filterbeslutning(logger, Level.ERROR);

            // Assert
            assertThat(beslutning).as(logger).isEqualTo(FilterReply.NEUTRAL);
        }
    }

    @Test
    public void andreLoggereSkalIkkeFiltreresBort() throws Exception {
        // Arrange
        lastStdoutKonfigurasjon();

        for (String logger : List.of("no.nav.app", "org.apache", "no.nav.common", "org.apache.CXF", "")) {
            for (Level level : List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR)) {
                // Act
                FilterReply beslutning = filterbeslutning(logger, level);

                // Assert
                assertThat(beslutning).as("%s med nivaa %s", logger, level).isEqualTo(FilterReply.NEUTRAL);
            }
        }
    }

    @Test
    public void cxfSecureLogSkalVaereAvNaarEgenskapenMangler() throws Exception {
        // Arrange
        var loggere = List.of("org.apache.cxf", "no.nav.common.cxf");

        // Act
        lastCxfKonfigurasjon();

        // Assert
        for (String logger : loggere) {
            assertThat(context.getLogger(logger).getLevel()).isEqualTo(Level.ERROR);
            assertThat(context.getLogger(logger).getAppender("secureLog")).isNull();
            assertThat(context.getLogger(logger).getAppender("stdout_json_masked"))
                    .isNull();
        }
        assertIngenKonfigurasjonsfeil();
    }

    @Test
    public void cxfSecureLogSkalAktiveresNaarEgenskapenErDefinert() throws Exception {
        // Arrange
        context.putProperty("CXF_SECURE_LOG", "true");

        // Act
        lastCxfKonfigurasjon();

        // Assert
        assertCxfSecureLog(Level.INFO);
    }

    @Test
    public void cxfSecureLogSkalBeholdeSjekkForDefinertIkkeBoolskVerdi() throws Exception {
        // Arrange
        context.putProperty("CXF_SECURE_LOG", "false");
        context.putProperty("CXF_SECURE_LOG_LEVEL", "DEBUG");

        // Act
        lastCxfKonfigurasjon();

        // Assert
        assertCxfSecureLog(Level.DEBUG);
    }

    @Test
    public void janinoOgCommonsCompilerSkalIkkeVaerePaaKlassebanen() {
        // Arrange
        var klasser = List.of(
                "org/codehaus/janino/ScriptEvaluator.class",
                "org/codehaus/commons/compiler/IExpressionEvaluator.class");

        // Act
        var klasserPaaKlassebanen = klasser.stream()
                .map(getClass().getClassLoader()::getResource)
                .filter(Objects::nonNull)
                .toList();

        // Assert
        assertThat(klasserPaaKlassebanen).isEmpty();
    }

    private void lastStdoutKonfigurasjon() throws Exception {
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(requireNonNull(getClass().getResource("/logback-test.xml")));
    }

    private void lastCxfKonfigurasjon() throws Exception {
        String xml = """
                <configuration>
                    <include resource="no/nav/common/log/logback-stdout-json.xml"/>
                    <appender name="secureLog" class="ch.qos.logback.core.read.ListAppender"/>
                    <include resource="no/nav/common/log/logback-cxf.xml"/>
                </configuration>
                """;
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try (var input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            configurator.doConfigure(input);
        }
    }

    private Appender<ILoggingEvent> stdoutAppender() {
        return context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getAppender("stdout_json_masked");
    }

    private FilterReply filterbeslutning(String logger, Level level) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerName(logger);
        event.setLevel(level);
        return stdoutAppender().getFilterChainDecision(event);
    }

    private void assertCxfSecureLog(Level level) {
        for (String logger : List.of("org.apache.cxf", "no.nav.common.cxf")) {
            assertThat(context.getLogger(logger).getLevel()).isEqualTo(level);
            assertThat(context.getLogger(logger).getAppender("secureLog")).isNotNull();
            assertThat(context.getLogger(logger).getAppender("stdout_json_masked"))
                    .isSameAs(stdoutAppender());
        }
        assertIngenKonfigurasjonsfeil();
    }

    private void assertIngenKonfigurasjonsfeil() {
        assertThat(context.getStatusManager().getCopyOfStatusList())
                .filteredOn(status -> status.getEffectiveLevel() >= Status.ERROR)
                .isEmpty();
    }
}
