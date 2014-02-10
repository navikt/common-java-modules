package no.nav.sbl.dialogarena.common.web.selftest;

import no.nav.sbl.dialogarena.types.Pingable;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.jar.Manifest;

import static no.nav.modig.lang.collections.IterUtils.on;
import static no.nav.modig.lang.collections.PredicateUtils.not;
import static no.nav.sbl.dialogarena.types.Pingable.Ping;
import static org.apache.commons.lang3.StringUtils.join;

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
        result = on(getPingables()).map(PING).collect();
    }

    protected String getStatus() {
        return on(result).filter(not(VELLYKKET)).isEmpty() ? "OK" : "ERROR";
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
        return join(on(result).filter(not(VELLYKKET)).map(KOMPONENT).collect(), ",");
    }

    private static final Transformer<Pingable, Ping> PING = new Transformer<Pingable, Ping>() {
        @Override
        public Ping transform(Pingable pingable) {
            long startTime = System.currentTimeMillis();
            Ping ping = pingable.ping();
            ping.setTidsbruk(System.currentTimeMillis() - startTime);
            if (!ping.isVellykket()) {
                logger.warn("Feil ved SelfTest av " + ping.getKomponent(), ping.getAarsak());
            }
            return ping;
        }
    };

    private static final Transformer<Ping, String> KOMPONENT = new Transformer<Ping, String>() {
        @Override
        public String transform(Ping ping) {
            return ping.getKomponent();
        }
    };

    private static final Predicate<Ping> VELLYKKET = new Predicate<Ping>() {
        @Override
        public boolean evaluate(Ping ping) {
            return ping.isVellykket();
        }
    };
}
