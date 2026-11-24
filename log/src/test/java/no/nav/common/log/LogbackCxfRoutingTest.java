package no.nav.common.log;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.read.ListAppender;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class LogbackCxfRoutingTest {

    private static final String APACHE_MELDING = "ufarlig-cxf-testmelding-apache";
    private static final String COMMON_MELDING = "ufarlig-cxf-testmelding-common";
    private final LoggerContext context = new LoggerContext();
    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    @AfterEach
    public void stoppLoggerkontekst() {
        context.stop();
    }

    @Test
    public void gammeltJaninoOppsettSlipperCxfInfoTilStdoutNaarSecurelogErAktivert() throws Exception {
        // Arrange
        context.putProperty("CXF_SECURE_LOG", "true");
        lastKonfigurasjon("logback-stdout-json-janino.xml");
        var secureMeldinger = secureMeldinger();

        // Act
        loggCxfInfo();
        context.stop();

        // Assert
        assertThat(stdout.toString(StandardCharsets.UTF_8)).contains(APACHE_MELDING, COMMON_MELDING);
        assertThat(secureMeldinger)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(APACHE_MELDING, COMMON_MELDING);
        assertThat(context.getStatusManager().getCopyOfStatusList())
                .anySatisfy(status -> assertThat(status.getMessage()).contains("stub for JaninoEventEvaluator"));
    }

    @Test
    public void nyEvaluatorHolderCxfInfoUtenforStdoutMenBeholderSecurelog() throws Exception {
        // Arrange
        context.putProperty("CXF_SECURE_LOG", "true");
        lastKonfigurasjon("no/nav/common/log/logback-stdout-json.xml");
        var secureMeldinger = secureMeldinger();

        // Act
        loggCxfInfo();
        context.getLogger("no.nav.app.Test").info("ufarlig-vanlig-testmelding");
        context.stop();

        // Assert
        assertThat(stdout.toString(StandardCharsets.UTF_8))
                .doesNotContain(APACHE_MELDING, COMMON_MELDING)
                .contains("ufarlig-vanlig-testmelding");
        assertThat(secureMeldinger)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(APACHE_MELDING, COMMON_MELDING);
    }

    @Test
    public void gammeltOppsettStopperCxfInfoPaaLoggernivaaUtenSecurelog() throws Exception {
        // Arrange
        lastKonfigurasjon("logback-stdout-json-janino.xml");

        // Act
        boolean apacheInfoAktivert = context.getLogger("org.apache.cxf").isInfoEnabled();
        boolean commonInfoAktivert = context.getLogger("no.nav.common.cxf").isInfoEnabled();
        loggCxfInfo();
        context.getLogger("no.nav.app.Test").info("ufarlig-vanlig-testmelding");
        context.stop();

        // Assert
        assertThat(apacheInfoAktivert).isFalse();
        assertThat(commonInfoAktivert).isFalse();
        assertThat(stdout.toString(StandardCharsets.UTF_8))
                .doesNotContain(APACHE_MELDING, COMMON_MELDING)
                .contains("ufarlig-vanlig-testmelding");
    }

    @Test
    public void nyEvaluatorSlipperFortsattCxfErrorTilStdoutOgSecurelog() throws Exception {
        // Arrange
        context.putProperty("CXF_SECURE_LOG", "true");
        lastKonfigurasjon("no/nav/common/log/logback-stdout-json.xml");
        var secureMeldinger = secureMeldinger();

        // Act
        context.getLogger("org.apache.cxf.services.TestService").error(APACHE_MELDING);
        context.getLogger("no.nav.common.cxf.TestInterceptor").error(COMMON_MELDING);
        context.stop();

        // Assert
        assertThat(stdout.toString(StandardCharsets.UTF_8)).contains(APACHE_MELDING, COMMON_MELDING);
        assertThat(secureMeldinger)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(APACHE_MELDING, COMMON_MELDING);
    }

    private void lastKonfigurasjon(String stdoutResource) throws Exception {
        context.setMDCAdapter(new LogbackMDCAdapter());
        String xml = """
                <configuration>
                    <include resource="%s"/>
                    <appender name="secureLog" class="ch.qos.logback.core.read.ListAppender"/>
                    <include resource="no/nav/common/log/logback-cxf.xml"/>
                </configuration>
                """.formatted(stdoutResource);
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try (var input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            configurator.doConfigure(input);
        }
        var console = (ConsoleAppender<ILoggingEvent>) stdoutAppender().getAppender("consoleAppender");
        console.setOutputStream(stdout);
    }

    private MaskingAppender stdoutAppender() {
        return (MaskingAppender)
                context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getAppender("stdout_json_masked");
    }

    private List<ILoggingEvent> secureMeldinger() {
        var secure = (ListAppender<ILoggingEvent>)
                context.getLogger("org.apache.cxf").getAppender("secureLog");
        return secure.list;
    }

    private void loggCxfInfo() {
        context.getLogger("org.apache.cxf.services.TestService").info(APACHE_MELDING);
        context.getLogger("no.nav.common.cxf.TestInterceptor").info(COMMON_MELDING);
    }
}
