package no.nav.common.health.selftest;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

public class SelftestHtmlGenerator {

    private final static String htmlTemplate = readResourceFile("/selftest/SelfTestPage.html");

    public static String generate(List<SelftTestCheckResult> checkResults, String host, LocalDateTime timestamp) {
        List<String> tabellrader = checkResults
                .stream()
                .map(SelftestHtmlGenerator::lagTabellrad)
                .collect(Collectors.toList());

        String html = htmlTemplate;
        html = html.replace("${aggregertStatus}", getStatusNavnElement(SelfTestUtils.aggregateStatus(checkResults), "span"));
        html = html.replace("${resultater}", String.join("\n", tabellrader));
        html = html.replace("${host}", "Host: " + host);
        html = html.replace("${generert-tidspunkt}", timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return html;
    }

    private static String getStatusNavnElement(SelfTestStatus selfTestStatus, String nodeType) {
        switch (selfTestStatus == null ? SelfTestStatus.ERROR : selfTestStatus) {
            case ERROR:
                return getHtmlNode(nodeType, "roundSmallBox error", "ERROR");
            case WARNING:
                return getHtmlNode(nodeType, "roundSmallBox warning", "WARNING");
            case OK:
            default:
                return getHtmlNode(nodeType, "roundSmallBox ok", "OK");
        }
    }

    private static String getHtmlNode(String nodeType, String classes, String content) {
        return MessageFormat.format("<{0} class=\"{1}\">{2}</{0}>", nodeType, classes, content);
    }

    private static String lagTabellrad(SelftTestCheckResult result) {
        String status = getStatusNavnElement(SelfTestUtils.toStatus(result), "div");
        String kritisk = result.selfTestCheck.isCritical() ? "Ja" : "Nei";

        return tableRow(
                status,
                kritisk,
                result.timeUsed + " ms",
                result.selfTestCheck.getDescription(),
                getFeilmelding(result.checkResult)
        );
    }

    private static String getFeilmelding(HealthCheckResult checkResult) {
        if (checkResult.isHealthy()) {
            return "";
        }

        String feilmelding = "";

        Optional<String> maybeErrorMessage = checkResult.getErrorMessage();
        Optional<Throwable> maybeError = checkResult.getError();

        if (maybeErrorMessage.isPresent()) {
            feilmelding += getHtmlNode("p", "feilmelding", maybeErrorMessage.get());
        }

        if (maybeError.isPresent()) {
            feilmelding += getHtmlNode("p", "stacktrace", maybeError.get().toString());
        }

        return feilmelding;
    }

    private static String tableRow(Object... tdContents) {
        String row = Arrays.stream(tdContents)
                .map(o -> ofNullable(o).map(Object::toString).orElse(""))
                .collect(joining("</td><td>"));
        return "<tr><td>" + row +  "</td></tr>\n";

    }

    @SneakyThrows
    private static String readResourceFile(String fileName) {
        try (InputStream resourceStream = SelftestHtmlGenerator.class.getResourceAsStream(fileName)) {
            return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
