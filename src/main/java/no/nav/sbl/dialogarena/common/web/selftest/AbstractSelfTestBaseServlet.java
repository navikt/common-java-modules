package no.nav.sbl.dialogarena.common.web.selftest;

import no.nav.sbl.dialogarena.types.Pingable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Manifest;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.nav.sbl.dialogarena.types.Pingable.Ping;

public abstract class AbstractSelfTestBaseServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSelfTestBaseServlet.class);


    protected List<Ping> result;

    /**
     * Denne metoden må implementeres til å returnere applikasjonens navn, for bruk i tittel og overskrift
     * på selftestsiden.
     * @return Applikasjonens navn
     */
    protected abstract String getApplicationName();

    /**
     * Denne metoden må implementeres til å returnere en Collection av alle tjenester som skal inngå
     * i selftesten. Tjenestene må implementere Pingable-grensesnittet.
     * @return Liste over tjenester som implementerer Pingable
     */
    protected abstract Collection<? extends Pingable> getPingables();

    protected void doPing() {
        result = getPingables().stream().map(PING).collect(toList());
    }

    protected Integer getAggregertStatus() {
        boolean harKritiskFeil = result.stream().anyMatch(KRITISK_FEIL);
        boolean harFeil = result.stream().anyMatch(HAR_FEIL);

        if (harKritiskFeil) {
            return STATUS_ERROR;
        } else if (harFeil) {
            return STATUS_WARNING;
        }
        return STATUS_OK;
    }

    protected String getApplicationVersion() {
        String version = "unknown version";
        try {
            InputStream inputStream = getServletContext().getResourceAsStream(("/META-INF/MANIFEST.MF"));
            version = new Manifest(inputStream).getMainAttributes().getValue("Implementation-Version");
        } catch (Exception e) {
            logger.warn("Feil ved henting av applikasjonsversjon: " + e.getMessage());
        }
        return version;
    }

    protected String getHost() {
        String host = "unknown host";
        try {
            host = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            logger.error("Error retrieving host", e);
        }
        return host;
    }

    protected String getMessage() {
        return result.stream()
                .filter(HAR_FEIL)
                .map(ENDEPUNKT)
                .collect(joining(","));
    }

    private static boolean harKritiskFeil(List<Ping> resultater) {
        return resultater.stream()
                .anyMatch(KRITISK_FEIL);
    }

    private static final Function<Pingable, Ping> PING = pingable -> {
        long startTime = System.currentTimeMillis();
        Ping ping = pingable.ping();
        ping.setResponstid(System.currentTimeMillis() - startTime);
        if (!ping.erVellykket()) {
            logger.warn("Feil ved SelfTest av " + ping.getEndepunkt(), ping.getFeil());
        }
        return ping;
    };

    private static final Function<Ping, String> ENDEPUNKT = Ping::getEndepunkt;
    private static final Predicate<Ping> VELLYKKET = Ping::erVellykket;
    private static final Predicate<Ping> KRITISK_FEIL = ping -> ping.harFeil() && ping.erKritisk();
    private static final Predicate<Ping> HAR_FEIL = Ping::harFeil;

    static final int STATUS_OK = 0;
    static final int STATUS_ERROR = 1;
    static final int STATUS_WARNING = 2;
}
