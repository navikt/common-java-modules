package no.nav.common.log;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.core.joran.spi.JoranException;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;

@Slf4j(topic = "SecureLog")
public class LogbackSecureLogsTest {
    public static final String HEMMELIG_MELDING = "Hemmelig melding med fnr: 10108000398";

    @Rule
    public final TemporaryFolder securelogsDir = new TemporaryFolder();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Test
    public void skal_logge_json_til_securelogs() throws IOException, JoranException {
        System.setProperty("SECURELOGS_DIR", securelogsDir.getRoot().getAbsolutePath());
        LogTestHelpers.loadLogbackConfig("/logback-securelogs-test.xml");

        String melding = HEMMELIG_MELDING + " " + UUID.randomUUID();
        log.info(melding);

        LogTestHelpers.flushLogs();

        String logMessages = Files.readString(securelogsDir.getRoot().toPath().resolve("secure.log"));

        Gson gson = new Gson();
        Optional<LogLinje> lastSecurelogMessage = Arrays.stream(logMessages.split("\n"))
                .map(l -> gson.fromJson(l, LogLinje.class))
                .reduce((logLinje, logLinje2) -> logLinje2);

        assertThat(lastSecurelogMessage)
                .hasValueSatisfying(logLinje -> assertThat(logLinje.message).isEqualTo(melding));
    }
}
