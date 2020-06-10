package no.nav.common.health.selftest;

import no.nav.common.health.HealthCheckResult;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.checkAll;
import static org.junit.Assert.assertEquals;

public class SelftestHtmlGeneratorTest {

    @Test
    public void should_generate_selftest_html() throws IOException {
        List<SelfTestCheck> selftestChecks = List.of(
                new SelfTestCheck("Check 1", false, HealthCheckResult::healthy),
                new SelfTestCheck("Check 2", true, () -> HealthCheckResult.unhealthy("Something went wrong :("))
        );

        List<SelftTestCheckResult> checkResults = checkAll(selftestChecks);

        String selftestHtml = SelftestHtmlGenerator.generate(checkResults, "test", LocalDateTime.MIN);
        String cleanedSelfTestHtml = cleanHtml(selftestHtml).replaceAll("\\dms", "Xms");

        String expectedSelftestHtml = new String(this.getClass().getResourceAsStream("/expected-selftest.html").readAllBytes());
        String cleanedExpectedSelftestHtml = cleanHtml(expectedSelftestHtml);

        assertEquals(cleanedExpectedSelftestHtml, cleanedSelfTestHtml);
    }

    private static String cleanHtml(String html) {
        return html.replace(" ", "")
                .replace("\t", "")
                .replace("\r", "");
    }

}
