package no.nav.common.log;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.core.joran.spi.JoranException;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Slf4j(topic = "SecureLog")
public class LogbackSecureLogsTest {
    public static final String HEMMELIG_MELDING = "Hemmelig melding med fnr: 10108000398";

    @TempDir
    public Path securelogsDir;

    private Properties originalProperties;

    @BeforeEach
    public void lagre_systemegenskaper() {
        originalProperties = (Properties) System.getProperties().clone();
    }

    @AfterEach
    public void gjenopprett_systemegenskaper() {
        System.setProperties(originalProperties);
    }

    @Test
    public void skal_logge_json_til_securelogs() throws IOException, JoranException {
        System.setProperty("SECURELOGS_DIR", securelogsDir.toAbsolutePath().toString());
        LogTestHelpers.loadLogbackConfig("/logback-securelogs-test.xml");

        String melding = HEMMELIG_MELDING + " " + UUID.randomUUID();
        log.info(melding);

        LogTestHelpers.flushLogs();

        String logMessages = Files.readString(securelogsDir.resolve("secure.log"));

        Gson gson = new Gson();
        Optional<LogLinje> lastSecurelogMessage = Arrays.stream(logMessages.split("\n"))
                .map(l -> gson.fromJson(l, LogLinje.class))
                .reduce((logLinje, logLinje2) -> logLinje2);

        assertThat(lastSecurelogMessage)
                .hasValueSatisfying(logLinje -> assertThat(logLinje.message).isEqualTo(melding));
    }
}
