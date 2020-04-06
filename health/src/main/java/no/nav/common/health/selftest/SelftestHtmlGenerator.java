package no.nav.common.health.selftest;


import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static no.nav.common.health.selftest.SelfTestStatus.ERROR;
import static no.nav.common.health.selftest.SelfTestStatus.OK;
import static org.apache.commons.lang3.StringUtils.join;

public class SelftestHtmlGenerator {
    public static String generate(Selftest selftest, String host) throws IOException {
        Selftest selftestNullSafe = ofNullable(selftest).orElseGet(() -> Selftest.builder().build());
        List<SelftestResult> checks = selftestNullSafe.getChecks();
        List<String> feilendeKomponenter = checks.stream()
                .filter(SelftestResult::harFeil)
                .map(SelftestResult::getEndpoint)
                .collect(Collectors.toList());

        List<String> tabellrader = checks.stream()
                .map(SelftestHtmlGenerator::lagTabellrad)
                .collect(Collectors.toList());

        InputStream template = SelftestHtmlGenerator.class.getResourceAsStream("/selftest/SelfTestPage.html");
        String html = IOUtils.toString(template);
        html = html.replace("${app-navn}", ofNullable(selftestNullSafe).map(s -> selftestNullSafe.getApplication()).orElse("?"));
        html = html.replace("${aggregertStatus}", getStatusNavnElement(selftestNullSafe.getAggregateResult(), "span"));
        html = html.replace("${resultater}", join(tabellrader, "\n"));
        html = html.replace("${version}", selftestNullSafe.getApplication() + "-" + selftestNullSafe.getVersion());
        html = html.replace("${host}", "Host: " + host);
        html = html.replace("${generert-tidspunkt}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        html = html.replace("${feilende-komponenter}", join(feilendeKomponenter, ", "));

        return html;
    }

    private static String getStatusNavnElement(SelfTestStatus selfTestStatus, String nodeType) {
        switch (selfTestStatus == null ? ERROR : selfTestStatus) {
            case ERROR:
                return getHtmlNode(nodeType, "roundSmallBox error", "ERROR");
            case WARNING:
                return getHtmlNode(nodeType, "roundSmallBox warning", "WARNING");
            case DISABLED:
                return getHtmlNode(nodeType, "roundSmallBox avskrudd", "OFF");
            case OK:
            default:
                return getHtmlNode(nodeType, "roundSmallBox ok", "OK");
        }
    }

    private static String getHtmlNode(String nodeType, String classes, String content) {
        return MessageFormat.format("<{0} class=\"{1}\">{2}</{0}>", nodeType, classes, content);
    }

    private static String lagTabellrad(SelftestResult endpoint) {
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

    private static String getFeilmelding(SelftestResult endpoint) {
        if (endpoint.getResult() == OK) {
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
                .map(o -> ofNullable(o).map(Object::toString).orElse(""))
                .collect(joining("</td><td>"));
        return "<tr><td>" + row +  "</td></tr>\n";

    }
}
