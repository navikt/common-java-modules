package no.nav.common.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


public class LogbackSecureLogsTest {

    private static final String SECURELOGS_DIR = "./target/securelogs";
    public static final String HEMMELIG_MELDING = "Hemmelig melding med fnr: 10108000398";

    @Rule
    public final ProvideSystemProperty securelogsDir
            = new ProvideSystemProperty("SECURELOGS_DIR", SECURELOGS_DIR);
    @Rule
    public final RestoreSystemProperties restoreSystemProperties
            = new RestoreSystemProperties();

    @Test
    @SneakyThrows
    public void skal_logge_json_til_securelogs() {

        loadLogbackConfig("/logback-securelogs-test.xml");

        Logger secureLogs = LoggerFactory.getLogger("SecureLog");

        secureLogs.info(HEMMELIG_MELDING);


        flushLogs();
        String logMessages = new String(Files.readAllBytes(Paths.get(SECURELOGS_DIR + "/secure.log")));

        Gson gson = new Gson();
        Optional<LogLinje> lastSecurelogMessage = Arrays.stream(logMessages
                .split("\n"))
                .map(l -> gson.fromJson(l, LogLinje.class))
                .reduce((logLinje, logLinje2) -> logLinje2);



        assertThat(lastSecurelogMessage.get().message).isEqualTo(HEMMELIG_MELDING);
    }



    private void loadLogbackConfig(String path) throws JoranException {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        // Sett konfigurasjonsfilen for LoggerContext

        URL configUrl = getClass().getResource(path);
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);configurator.doConfigure(configUrl);
        StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    }

    private static void flushLogs() {
        LoggerContext loggerContext1 = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext1.stop();
    }
}
