package no.nav.sbl.dialogarena.common.web.selftest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

        Result(String html, List<String> errors) {
            this.html = html;
            this.errors = errors;
        }
    }

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
     * inneholde nøyaktig seks <code><td></td></code>-elementer.
     * @return Ett eller flere <code><tr></tr></code>-elementer
     */
    protected String getCustomRows() {
        return "";
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Result result = kjorSelfTest();
        resp.setContentType("text/html");
        resp.getWriter().write(result.html);
    }

    private Result kjorSelfTest() throws IOException {
        doPing();

        List<String> feilendeKomponenter = this.result.stream()
                .filter(Ping::harFeil)
                .map(Ping::getEndepunkt)
                .collect(Collectors.toList());

        List<String> tabellrader = this.result.stream()
                .map(SelfTestBaseServlet::lagTabellrad)
                .collect(Collectors.toList());

        try (InputStream template = getClass().getResourceAsStream("/selftest/SelfTestPage.html")) {
            String html = IOUtils.toString(template);
            html = html.replace("${app-navn}", getApplicationName());
            html = html.replace("${aggregertStatus}", getStatusNavnElement(getAggregertStatus(), "span"));
            html = html.replace("${resultater}", join(tabellrader, "\n"));
            html = html.replace("${custom-rows}", StringUtils.defaultString(getCustomRows()));
            html = html.replace("${version}", getApplicationName() + "-" + getApplicationVersion());
            html = html.replace("${host}", "Host: " + getHost());
            html = html.replace("${generert-tidspunkt}", DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            html = html.replace("${feilende-komponenter}", join(feilendeKomponenter, ", "));
            html = html.replace("${custom-markup}", StringUtils.defaultString(getCustomMarkup()));

            return new Result(html, feilendeKomponenter);
        }
    }

    private static String getStatusNavnElement(Integer statuskode, String nodeType) {
        switch(statuskode) {
            case STATUS_ERROR:
                return getHtmlNode(nodeType, "roundSmallBox error", "ERROR");
            case STATUS_WARNING:
                return getHtmlNode(nodeType, "roundSmallBox warning", "WARNING");
            case STATUS_OK:
            default:
                return getHtmlNode(nodeType, "roundSmallBox ok", "OK");
        }
    }

    private static String getHtmlNode(String nodeType, String classes, String content) {
        return MessageFormat.format("<{0} class=\"{1}\">{2}</{0}>", nodeType, classes, content);
    }

    private static String lagTabellrad(Ping ping) {
        String status = getStatusNavnElement(ping.harFeil() ? STATUS_ERROR : STATUS_OK, "div");
        String kritisk = ping.erKritisk() ? "Ja" : "Nei";

        return tableRow(status, kritisk, ping.getResponstid() + " ms", ping.getBeskrivelse(), ping.getEndepunkt(), getFeilmelding(ping));
    }

    private static String getFeilmelding(Ping ping) {
        if (ping.erVellykket()) {
            return "";
        }

        String feilmelding = getHtmlNode("p", "feilmelding", ping.getAarsak());
        if (ping.getFeil() != null) {
            feilmelding += getHtmlNode("p", "stacktrace", ExceptionUtils.getStackTrace(ping.getFeil()));
        }
        return feilmelding;
    }

    private static String tableRow(Object... tdContents) {
        String row = Arrays.stream(tdContents)
                .map(Object::toString)
                .collect(joining("</td><td>"));
        return "<tr><td>" + row +  "</td></tr>\n";

    }
}