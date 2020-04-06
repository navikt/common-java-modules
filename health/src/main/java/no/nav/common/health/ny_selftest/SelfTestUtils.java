package no.nav.common.health.ny_selftest;

import no.nav.common.health.HealthCheckResult;

import java.util.List;
import java.util.stream.Collectors;

public class SelfTestUtils {

    public static List<SelftTestCheckResult> test(List<SelfTestCheck> checks) {
        return checks.stream()
                .map(SelfTestUtils::performSelftTestCheck)
                .collect(Collectors.toList());
    }

    public static SelfTestStatus toStatus(SelftTestCheckResult result) {
        boolean isHealthy = result.checkResult.isHealthy();
        boolean isCritical = result.selfTestCheck.isCritical();

        if (isHealthy) {
            return SelfTestStatus.OK;
        }

        return isCritical ? SelfTestStatus.ERROR : SelfTestStatus.WARNING;
    }

    public static SelfTestStatus aggregateStatus(List<SelftTestCheckResult> checkResults) {
        List<SelfTestStatus> statuses = checkResults.stream()
                .map(SelfTestUtils::toStatus)
                .collect(Collectors.toList());

        boolean hasError = statuses.stream().anyMatch(s -> s == SelfTestStatus.ERROR);
        boolean hasWarning = statuses.stream().anyMatch(s -> s == SelfTestStatus.WARNING);

        if (hasError) {
            return SelfTestStatus.ERROR;
        } else if (hasWarning) {
            return SelfTestStatus.WARNING;
        }

        return SelfTestStatus.OK;
    }

    private static SelftTestCheckResult performSelftTestCheck(SelfTestCheck check) {
        long beforeCheck = System.currentTimeMillis();
        HealthCheckResult result = check.getCheck().checkHealth();
        long timeUsed = System.currentTimeMillis() - beforeCheck;

        return new SelftTestCheckResult(check, result, timeUsed);
    }

}
