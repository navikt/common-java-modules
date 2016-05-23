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
     * Denne metoden må implementeres til å returnere en Collection av alle tjenester som skal inngå
     * i selftesten. Tjenestene må implementere Pingable-grensesnittet.
     * @return Liste over tjenester som implementerer Pingable
     */
    protected abstract Collection<? extends Pingable> getPingables();

    protected void doPing() {
        result = getPingables().stream().map(PING).collect(toList());
    }

    protected String getStatus() {
        return result.stream()
                .filter(IKKE_VELLYKKET)
                .findAny().isPresent() ? "ERROR" : "OK";
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
                .filter(IKKE_VELLYKKET)
                .map(KOMPONENT)
                .collect(joining(","));
    }

    private static final Function<Pingable, Ping> PING = pingable -> {
        long startTime = System.currentTimeMillis();
        Ping ping = pingable.ping();
        ping.setTidsbruk(System.currentTimeMillis() - startTime);
        if (!ping.isVellykket()) {
            logger.warn("Feil ved SelfTest av " + ping.getKomponent(), ping.getAarsak());
        }
        return ping;
    };

    private static final Function<Ping, String> KOMPONENT = ping -> ping.getKomponent();
    private static final Predicate<Ping> VELLYKKET = ping -> ping.isVellykket();
    private static final Predicate<Ping> IKKE_VELLYKKET = ping -> !ping.isVellykket();
}
