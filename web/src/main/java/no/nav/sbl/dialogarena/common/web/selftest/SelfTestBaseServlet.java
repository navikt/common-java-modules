package no.nav.sbl.dialogarena.common.web.selftest;

import no.nav.sbl.dialogarena.common.web.selftest.domain.Selftest;
import no.nav.sbl.dialogarena.common.web.selftest.domain.SelftestEndpoint;
import no.nav.sbl.dialogarena.common.web.selftest.generators.SelftestHtmlGenerator;
import no.nav.sbl.dialogarena.common.web.selftest.generators.SelftestJsonGenerator;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.util.EnvironmentUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static no.nav.sbl.dialogarena.types.Pingable.Ping;
import static no.nav.sbl.util.EnvironmentUtils.getApplicationName;
import static no.nav.sbl.util.EnvironmentUtils.requireApplicationName;

public abstract class SelfTestBaseServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(SelfTestBaseServlet.class);

    protected List<Ping> result;
    private volatile long lastResultTime;

    /**
     * Denne metoden må implementeres til å returnere en Collection av alle tjenester som skal inngå
     * i selftesten. Tjenestene må implementere Pingable-grensesnittet.
     * @return Liste over tjenester som implementerer Pingable
     */
    protected abstract Collection<? extends Pingable> getPingables();

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPing();
        Selftest selftest = lagSelftest();

        if ("application/json".equalsIgnoreCase(req.getHeader("accept"))) {
            resp.setContentType("application/json");
            resp.getWriter().write(SelftestJsonGenerator.generate(selftest));
        } else {
            resp.setContentType("text/html");
            resp.getWriter().write(SelftestHtmlGenerator.generate(selftest, getHost()));
        }
    }

    protected void doPing() {
        long requestTime = System.currentTimeMillis();
        // Beskytter pingables mot mange samtidige/tette requester.
        // Særlig viktig hvis det tar lang tid å utføre alle pingables
        synchronized (this) {
            if (requestTime > lastResultTime) {
                result = getPingables().stream().map(PING).collect(toList());
                lastResultTime = System.currentTimeMillis();
            }
        }
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

    protected String getHost() {
        return EnvironmentUtils.resolveHostName();
    }

    private static final Function<Pingable, Ping> PING = pingable -> {
        long startTime = System.currentTimeMillis();
        Ping ping = pingable.ping();
        ping.setResponstid(System.currentTimeMillis() - startTime);
        if (!ping.erVellykket()) {
            logger.warn("Feil ved SelfTest av " + ping.getMetadata().getEndepunkt(), ping.getFeil());
        }
        return ping;
    };

    private Selftest lagSelftest() {
        return new Selftest()
            .setApplication(requireApplicationName())
            .setVersion(EnvironmentUtils.getApplicationVersion().orElse("?"))
            .setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            .setAggregateResult(getAggregertStatus())
            .setChecks(result.stream()
                    .map(SelfTestBaseServlet::lagSelftestEndpoint)
                    .collect(toList())
            );
}

    private static SelftestEndpoint lagSelftestEndpoint(Pingable.Ping ping) {
        return new SelftestEndpoint()
                .setEndpoint(ping.getMetadata().getEndepunkt())
                .setDescription(ping.getMetadata().getBeskrivelse())
                .setErrorMessage(ping.getFeilmelding())
                .setCritical(ping.getMetadata().isKritisk())
                .setResult(ping.harFeil() ? STATUS_ERROR : STATUS_OK)
                .setResponseTime(String.format("%dms", ping.getResponstid()))
                .setStacktrace(ofNullable(ping.getFeil())
                        .map(ExceptionUtils::getStackTrace)
                        .orElse(null)
                );
    }

    private static final Function<Ping, String> ENDEPUNKT = p -> p.getMetadata().getEndepunkt();
    private static final Predicate<Ping> VELLYKKET = Ping::erVellykket;
    private static final Predicate<Ping> KRITISK_FEIL = ping -> ping.harFeil() && ping.getMetadata().isKritisk();
    private static final Predicate<Ping> HAR_FEIL = Ping::harFeil;

    public static final int STATUS_OK = 0;
    public static final int STATUS_ERROR = 1;
    public static final int STATUS_WARNING = 2;
}
