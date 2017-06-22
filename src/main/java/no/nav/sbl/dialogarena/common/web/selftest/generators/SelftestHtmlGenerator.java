package no.nav.sbl.dialogarena.common.web.selftest.generators;

import no.nav.sbl.dialogarena.common.web.selftest.domain.Selftest;
import no.nav.sbl.dialogarena.common.web.selftest.domain.SelftestEndpoint;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet.STATUS_ERROR;
import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet.STATUS_OK;
import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet.STATUS_WARNING;
import static org.apache.commons.lang3.StringUtils.join;

public class SelftestHtmlGenerator {
    public static String generate(Selftest selftest, String host) throws IOException {
        List<String> feilendeKomponenter = selftest.getChecks().stream()
                .filter(SelftestEndpoint::harFeil)
                .map(SelftestEndpoint::getEndpoint)
                .collect(Collectors.toList());

        List<String> tabellrader = selftest.getChecks().stream()
                .map(SelftestHtmlGenerator::lagTabellrad)
                .collect(Collectors.toList());

        InputStream template = SelftestHtmlGenerator.class.getResourceAsStream("/selftest/SelfTestPage.html");
        String html = IOUtils.toString(template);
        html = html.replace("${app-navn}", selftest.getApplication());
        html = html.replace("${aggregertStatus}", getStatusNavnElement(selftest.getAggregateResult(), "span"));
        html = html.replace("${resultater}", join(tabellrader, "\n"));
        html = html.replace("${version}", selftest.getApplication() + "-" + selftest.getVersion());
        html = html.replace("${host}", "Host: " + host);
        html = html.replace("${generert-tidspunkt}", DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        html = html.replace("${feilende-komponenter}", join(feilendeKomponenter, ", "));

        return html;
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

    private static String lagTabellrad(SelftestEndpoint endpoint) {
        String status = getStatusNavnElement(endpoint.getResult(), "div");
        String kritisk = endpoint.isCritical() ? "Ja" : "Nei";

        return tableRow(
                status,
                kritisk,
                endpoint.getResponseTime(),
                endpoint.getDescription(),
                endpoint.getEndpoint(),
                getFeilmelding(endpoint)
        );
    }

    private static String getFeilmelding(SelftestEndpoint endpoint) {
        if (endpoint.getResult() == STATUS_OK) {
            return "";
        }

        String feilmelding = "";

        if (endpoint.getErrorMessage() != null) {
            feilmelding += getHtmlNode("p", "feilmelding", endpoint.getErrorMessage());
        }

        if (endpoint.getStacktrace() != null) {
            feilmelding += getHtmlNode("p", "stacktrace", endpoint.getStacktrace());
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
