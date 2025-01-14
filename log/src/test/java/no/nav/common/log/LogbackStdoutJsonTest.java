package no.nav.common.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter2;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;
import org.codehaus.commons.nullanalysis.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LogbackStdoutJsonTest {

    @Test
    @SneakyThrows
    public void fodselsnummerSkalMaskerer() {
        PrintStream out = System.out;

        LoadLogbackConfig("/logback-test.xml");
        ByteArrayOutputStream outputStream = captureSystemOut();

        Logger log = LoggerFactory.getLogger(LogbackStdoutJsonTest.class);

        String skalMaskeres = "dette er en test av maskereren 12345678910 kanskje den virker";
        String maskert = "dette er en test av maskereren *********** kanskje den virker";
        log.info(skalMaskeres);

        String skalIkkeMaskeres = "denne skal ikke maskerers 123456789123456789 eller kanskje den blir det?";
        log.info(skalIkkeMaskeres);

        flushLogs();

        String logtext = outputStream.toString();

        //da andre ting også logger når vi kjører testen må vi fjerne alle lingjer som ikke er json
        var logLinjes = hentLingjerSomStarterMedCurlyBraces(logtext);

        Assert.assertEquals("skal bare vere 2 log lingjer", 2, logLinjes.size());

        LogLinje skalVereMaskert = logLinjes.get(0);
        Assert.assertEquals(maskert, skalVereMaskert.message);

        LogLinje skalIkkeBliMaskert = logLinjes.get(1);
        Assert.assertEquals(skalIkkeMaskeres, skalIkkeBliMaskert.message);

        System.setOut(out);
    }


    @Test
    @SneakyThrows
    public void skal_logge_json_med_logbackStdoutJson() {
        PrintStream out = System.out;

        LoadLogbackConfig("/logback-test.xml");
        ByteArrayOutputStream outputStream = captureSystemOut();

        Logger log = LoggerFactory.getLogger(LogbackStdoutJsonTest.class);

        log.debug("Debug-melding");
        String infoMelding = "Info-melding";
        log.info(infoMelding);
        String advarselMelding = "Advarsel";
        log.warn(advarselMelding);
        String errorMelding = "Feilmelding";
        log.error(errorMelding);

        flushLogs();

        String logtext = outputStream.toString();

        //da andre ting også logger når vi kjører testen må vi fjerne alle lingjer som ikke er json
        var logLinjes = hentLingjerSomStarterMedCurlyBraces(logtext);

        Assert.assertEquals("skal være 3 loglingjer (ikke debug)", 3, logLinjes.size());

        LogLinje info = logLinjes.get(0);
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

    private static List<LogLinje> hentLingjerSomStarterMedCurlyBraces(String logtext) {
        Gson gson = new Gson();
        return Arrays.stream(logtext.split("\n"))
                .filter(l -> l.startsWith("{"))
                .map(l -> gson.fromJson(l, LogLinje.class))
                .toList();
    }


    private void LoadLogbackConfig(String path) throws JoranException {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        loggerContext.putProperty("testName", "LogbackTest");
        loggerContext.putProperty("logDirectory", "logs");
        // Sett konfigurasjonsfilen for LoggerContext

        URL configUrl = getClass().getResource(path);
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        configurator.doConfigure(configUrl);

        var statusPrinter = new StatusPrinter2();
        statusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    }

    @NotNull
    private static ByteArrayOutputStream captureSystemOut() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        return outputStream;
    }

    private static void flushLogs() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
        loggerContext.start();
    }
}
