package no.nav.sbl.dialogarena.common.web.selftest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static no.nav.sbl.dialogarena.types.Pingable.Ping;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * Base-servlet for selftestside uten bruk av Wicket.
 */
public abstract class SelfTestBaseServlet extends AbstractSelfTestBaseServlet {

    private static class Result {
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

    /**
     * Denne metoden må implementeres til å returnere applikasjonens navn, for bruk i tittel og overskrift
     * på selftestsiden.
     * @return Applikasjonens navn
     */
    protected abstract String getApplicationName();

    /**
     * Callback for å definere egen markup som dukker opp under tabellen over tjenestestatuser. Må
     * returnere gyldig HTML.
     * @return Egendefinert gyldig HTML.
     */
    protected String getCustomMarkup() {
        return "";
    }

    /**
     * Callback for å definere egne rader i tabellen med tjenestestatuser. Må returnere ett eller flere
     * <code><tr></tr></code>-elementer som ikke er wrappet i noe parent-element. Disse elementene må hver
     * inneholde nøyaktig fire <code><td></td></code>-elementer.
     * @return Ett eller flere <code><tr></tr></code>-elementer
     */
    protected String getCustomRows() {
        return "";
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Result result = kjorSelfTest();

        if (result.hasErrors()) {
            resp.setStatus(500);
        }
        resp.setContentType("text/html");
        resp.getWriter().write(result.html);
    }

    private Result kjorSelfTest() throws IOException {
        doPing();
        StringBuilder tabell = new StringBuilder();
        List<String> feilendeKomponenter = new ArrayList<>();
        String aggregertStatus = "OK";
        for (Ping resultat : this.result) {
            String status = resultat.isVellykket() ? "OK" : "ERROR";
            if (!resultat.isVellykket()) {
                aggregertStatus = "ERROR";
            }
            String beskrivelse = ofNullable(resultat.getAarsak()).map(MESSAGE).orElse("");
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
            html = html.replace("${custom-rows}", StringUtils.defaultString(getCustomRows()));
            html = html.replace("${version}", getApplicationName() + "-" + getApplicationVersion());
            html = html.replace("${host}", "Host: " + getHost());
            html = html.replace("${generert-tidspunkt}", DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            html = html.replace("${feilende-komponenter}", join(feilendeKomponenter, ","));
            html = html.replace("${custom-markup}", StringUtils.defaultString(getCustomMarkup()));

            return new Result(html, feilendeKomponenter);
        }
    }

    protected static String tableRow(Object... tdContents) {
        String row = asList(tdContents).stream()
                .map(o -> o.toString())
                .collect(joining("</td><td>"));
        return "<tr><td>" + row +  "</td></tr>\n";

    }

    private static final Function<Throwable, String> MESSAGE = throwable -> {
        StackTraceElement origin = throwable.getStackTrace()[0];
        return throwable.getClass().getSimpleName() + ": " + throwable.getMessage() + ", at " + origin.getClassName() + "." + origin.getMethodName()
                + "(..), line " + origin.getLineNumber();
    };
}