package no.nav.common.health.ny_selftest;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.join;

public class SelftestHtmlGenerator {

    private static String htmlTemplate = readResourceFile("/selftest/SelfTestPage.html");

    public static String generate(List<SelftTestCheckResult> checkResults, String host, LocalDateTime timestamp) {
        List<String> tabellrader = checkResults
                .stream()
                .map(SelftestHtmlGenerator::lagTabellrad)
                .collect(Collectors.toList());

        String html = htmlTemplate;
        html = html.replace("${aggregertStatus}", getStatusNavnElement(SelfTestUtils.aggregateStatus(checkResults), "span"));
        html = html.replace("${resultater}", join(tabellrader, "\n"));
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
                result.timeUsed,
                result.selfTestCheck.getDescription(),
                result.checkResult,
                getFeilmelding(result.checkResult)
        );
    }

    private static String getFeilmelding(HealthCheckResult checkResult) {
        if (checkResult.isHealthy()) {
            return "";
        }

        String feilmelding = "";

        if (checkResult.getErrorMessage() != null) {
            feilmelding += getHtmlNode("p", "feilmelding", checkResult.getErrorMessage());
        }

        if (checkResult.getError() != null) {
            feilmelding += getHtmlNode("p", "stacktrace", checkResult.getError().toString());
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
        URL fileUrl = SelftestHtmlGenerator.class.getClassLoader().getResource(fileName);
        Path resPath = Paths.get(fileUrl.toURI());
        return Files.readString(resPath);
    }

}
