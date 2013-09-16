package no.nav.sbl.dialogarena.common.web.selftest;

import no.nav.sbl.dialogarena.types.Pingable;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

import static java.util.Arrays.asList;
import static no.nav.modig.lang.collections.IterUtils.on;
import static no.nav.modig.lang.collections.TransformerUtils.joinedWith;
import static no.nav.modig.lang.option.Optional.optional;
import static no.nav.sbl.dialogarena.types.Pingable.Ping;
import static org.apache.commons.lang3.StringUtils.join;

public abstract class SelfTestBaseServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(SelfTestBaseServlet.class);

    static class Result {
        public final String html;
        public final List<String> errors;

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        Result(String html, List<String> errors) {
            this.html = html;
            this.errors = errors;
        }
    }

    protected abstract List<Pingable> getPingables();

    protected abstract String getApplicationName();

    protected String getCustomMarkup() {
        return "";
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Result result = kjorSelfTest(getPingables());

        if (result.hasErrors() && !req.getParameterMap().isEmpty()) {
            resp.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, join(result.errors, ", "));
        } else {
            resp.setContentType("text/html");
            resp.getWriter().write(result.html);
        }
    }

    private Result kjorSelfTest(List<Pingable> pingables) throws IOException {
        StringBuilder tabell = new StringBuilder();
        List<String> feilendeKomponenter = new ArrayList<>();
        String aggregertStatus = "OK";
        for (Ping resultat : on(pingables).map(SJEKK)) {
            String status = resultat.isVellykket() ? "OK" : "ERROR";
            if (!resultat.isVellykket()) {
                aggregertStatus = "ERROR";
            }
            String beskrivelse = optional(resultat.getAarsak()).map(MESSAGE).getOrElse("");
            tabell.append(tableRow(status, resultat.getKomponent(), resultat.getTidsbruk(), beskrivelse));
            if (!resultat.isVellykket()) {
                feilendeKomponenter.add(resultat.getKomponent());
            }
        }

        try (InputStream template = getClass().getResourceAsStream("/selftest/SelfTestPage.html")) {
            String html = IOUtils.toString(template);
            html = html.replace("${app-navn}", getApplicationName());
            html = html.replace("${aggregertStatus}", aggregertStatus);
            html = html.replace("${resultater}", tabell.toString());
            html = html.replace("${version}", getApplicationVersion());
            html = html.replace("${generert-tidspunkt}", DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            html = html.replace("${feilende-komponenter}", join(feilendeKomponenter, ","));
            html = html.replace("${custom-markup}", StringUtils.defaultString(getCustomMarkup()));

            return new Result(html, feilendeKomponenter);
        }
    }

    private static String tableRow(Object... tdContents) {
        return "<tr><td>" + optional(asList(tdContents)).map(joinedWith("</td><td>")).get() + "</td></tr>\n";
    }

    private static final Transformer<Pingable, Ping> SJEKK = new Transformer<Pingable, Ping>() {
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

    private String getApplicationVersion() {
        String version = "unknown version";
        try {
            InputStream inputStream = getServletContext().getResourceAsStream(("/META-INF/MANIFEST.MF"));
            version = new Manifest(inputStream).getMainAttributes().getValue("Implementation-Version");
        } catch (Exception e) {
            logger.warn("Feil ved henting av applikasjonsversjon: " + e.getMessage());
        }
        return getApplicationName() + "-" + version;
    }

    private static final Transformer<Throwable, String> MESSAGE = new Transformer<Throwable, String>() {
        @Override
        public String transform(Throwable throwable) {
            StackTraceElement origin = throwable.getStackTrace()[0];
            return throwable.getClass().getSimpleName() + ": " + throwable.getMessage() + ", at " + origin.getClassName() + "." + origin.getMethodName()
                    + "(..), line " + origin.getLineNumber();
        }
    };
}