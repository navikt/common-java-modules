package no.nav.common.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.Status;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class LogbackStdoutJsonTest {

    @Test
    @SneakyThrows
    public void fodselsnummerSkalMaskerer() {
        PrintStream out = System.out;

        LogTestHelpers.loadLogbackConfig("/logback-test.xml");
        ByteArrayOutputStream outputStream = captureSystemOut();

        Logger log = LoggerFactory.getLogger(LogbackStdoutJsonTest.class);

        String skalMaskeres = "dette er en test av maskereren 12345678910 kanskje den virker";
        String maskert = "dette er en test av maskereren *********** kanskje den virker";
        log.info(skalMaskeres);

        String skalIkkeMaskeres = "denne skal ikke maskerers 123456789123456789 eller kanskje den blir det?";
        log.info(skalIkkeMaskeres);

        LogTestHelpers.flushLogs();

        String logtext = outputStream.toString();

        //da andre ting også logger når vi kjører testen må vi fjerne alle lingjer som ikke er json
        var logLinjes = hentLinjerSomStarterMedCurlyBraces(logtext);

        Assert.assertEquals("skal bare vere 2 log lingjer", 2, logLinjes.size());

        LogLinje skalVereMaskert = logLinjes.get(0);
        Assert.assertEquals(maskert, skalVereMaskert.message);

        LogLinje skalIkkeBliMaskert = logLinjes.get(1);
        Assert.assertEquals(skalIkkeMaskeres, skalIkkeBliMaskert.message);

        System.setOut(out);
    }

    @Test
    @SneakyThrows
    public void loglinjeSkalInkludereMDCFelter() {
        PrintStream out = System.out;

        LogTestHelpers.loadLogbackConfig("/logback-test.xml");
        ByteArrayOutputStream outputStream = captureSystemOut();

        Logger log = LoggerFactory.getLogger(LogbackStdoutJsonTest.class);

        String mdcKey = UUID.randomUUID().toString();
        MDC.put(mdcKey, "value");
        log.info("en random logglinje");
        MDC.remove(mdcKey);

        LogTestHelpers.flushLogs();

        String logtext = outputStream.toString();

        // da andre ting også logger når vi kjører testen må vi fjerne alle lingjer som ikke er json
        var logLinjes = hentLinjerSomStarterMedCurlyBraces(logtext);
        Assert.assertEquals("skal bare vere 1 log lingjer", 1, logLinjes.size());
        Assert.assertTrue(logtext.contains(mdcKey));

        System.setOut(out);
    }

    @Test
    @SneakyThrows
    public void skal_logge_json_med_logbackStdoutJson() {
        PrintStream out = System.out;

        LogTestHelpers.loadLogbackConfig("/logback-test.xml");
        ByteArrayOutputStream outputStream = captureSystemOut();

        Logger log = LoggerFactory.getLogger(LogbackStdoutJsonTest.class);

        log.debug("Debug-melding");
        String infoMelding = "Info-melding";
        log.info(infoMelding);
        String advarselMelding = "Advarsel";
        log.warn(advarselMelding);
        String errorMelding = "Feilmelding";
        log.error(errorMelding);

        LogTestHelpers.flushLogs();

        String logtext = outputStream.toString();

        //da andre ting også logger når vi kjører testen må vi fjerne alle lingjer som ikke er json
        var logLinjes = hentLinjerSomStarterMedCurlyBraces(logtext);

        Assert.assertEquals("skal være 3 loglingjer (ikke debug)", 3, logLinjes.size());

        LogLinje info = logLinjes.getFirst();
        Assert.assertEquals(infoMelding, info.message);
        Assert.assertEquals("INFO", info.level);

        LogLinje warn = logLinjes.get(1);
        Assert.assertEquals(advarselMelding, warn.message);
        Assert.assertEquals("WARN", warn.level);

        LogLinje error = logLinjes.get(2);
        Assert.assertEquals(errorMelding, error.message);
        Assert.assertEquals("ERROR", error.level);

        //verifiser at ingen av feltene i logLingje er null
        logLinjes.forEach(l -> {
            Assert.assertNotNull(l.timestamp);
            Assert.assertNotNull(l.version);
            Assert.assertNotNull(l.message);
            Assert.assertNotNull(l.logger_name);
            Assert.assertNotNull(l.thread_name);
            Assert.assertNotNull(l.level);
            Assert.assertNotEquals(0, l.level_value);
        });

        System.setOut(out);
    }

    @Test
    @SneakyThrows
    public void cxf_loggere_skal_bare_logge_feil() {
        PrintStream out = System.out;

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getStatusManager().clear();
        LogTestHelpers.loadLogbackConfig("/logback-test.xml");
        var statuses = loggerContext.getStatusManager().getCopyOfStatusList();
        Assert.assertTrue(
                "Uventede advarsler eller feil ved Logback-konfigurasjon: " + statuses,
                statuses.stream().noneMatch(status -> status.getLevel() >= Status.WARN)
        );
        ByteArrayOutputStream outputStream = captureSystemOut();

        Logger cxfLog = LoggerFactory.getLogger("org.apache.cxf.client");
        Logger commonCxfLog = LoggerFactory.getLogger("no.nav.common.cxf.client");
        cxfLog.warn("CXF warning");
        cxfLog.error("CXF error");
        commonCxfLog.warn("common CXF warning");
        commonCxfLog.error("common CXF error");

        LogTestHelpers.flushLogs();

        var logLinjes = hentLinjerSomStarterMedCurlyBraces(outputStream.toString());
        Assert.assertEquals(2, logLinjes.size());
        Assert.assertEquals("CXF error", logLinjes.get(0).message);
        Assert.assertEquals("common CXF error", logLinjes.get(1).message);

        System.setOut(out);
    }

    private static List<LogLinje> hentLinjerSomStarterMedCurlyBraces(String logtext) {
        Gson gson = new Gson();
        return Arrays.stream(logtext.split("\n"))
                .filter(l -> l.startsWith("{"))
                .map(l -> gson.fromJson(l, LogLinje.class))
                .toList();
    }

    private static ByteArrayOutputStream captureSystemOut() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        return outputStream;
    }
}
