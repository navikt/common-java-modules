package no.nav.common.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.Status;
import com.google.gson.Gson;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class LogbackCxfSecureRoutingTest {

    private final String secureLogsDir = "./target/securelogs-cxf-" + UUID.randomUUID();

    @Rule
    public final ProvideSystemProperty cxfSecureLog
            = new ProvideSystemProperty("CXF_SECURE_LOG", "true");
    @Rule
    public final ProvideSystemProperty securelogsDirProperty
            = new ProvideSystemProperty("SECURELOGS_DIR", secureLogsDir);
    @Rule
    public final RestoreSystemProperties restoreSystemProperties
            = new RestoreSystemProperties();

    @Test
    public void cxf_secure_log_skal_rute_info_og_warn_kun_til_sikkerlogg() throws Exception {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getStatusManager().clear();

        PrintStream originalOut = System.out;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PrintStream capturedOut = new PrintStream(stdout);
        var secureLogPath = Paths.get(secureLogsDir, "secure.log");

        try {
            LogTestHelpers.loadLogbackConfig("/logback-cxf-secure-test.xml");

            var statuses = loggerContext.getStatusManager().getCopyOfStatusList();
            assertThat(statuses)
                    .noneMatch(status -> status.getLevel() >= Status.WARN);

            System.setOut(capturedOut);
            Logger cxfLogger = LoggerFactory.getLogger("org.apache.cxf.client");
            Logger commonCxfLogger = LoggerFactory.getLogger("no.nav.common.cxf.client");

            cxfLogger.info("CXF info");
            cxfLogger.warn("CXF warning");
            cxfLogger.error("CXF error");
            commonCxfLogger.info("common CXF info");
            commonCxfLogger.warn("common CXF warning");
            commonCxfLogger.error("common CXF error");

            LogTestHelpers.flushLogs();

            String secureLog = Files.readString(secureLogPath);
            assertThat(messagesFrom(secureLog)).containsExactlyInAnyOrder(
                    "CXF info",
                    "CXF warning",
                    "CXF error",
                    "common CXF info",
                    "common CXF warning",
                    "common CXF error"
            );

            List<String> stdoutMessages = messagesFrom(stdout.toString());
            assertThat(stdoutMessages).containsExactlyInAnyOrder("CXF error", "common CXF error");
        } finally {
            loggerContext.stop();
            System.setOut(originalOut);
            capturedOut.close();
            Files.deleteIfExists(secureLogPath);
            Files.deleteIfExists(secureLogPath.getParent());
        }
    }

    private static List<String> messagesFrom(String logText) {
        Gson gson = new Gson();
        return Arrays.stream(logText.split("\n"))
                .filter(line -> line.startsWith("{"))
                .map(line -> gson.fromJson(line, LogLinje.class).message)
                .toList();
    }
}
