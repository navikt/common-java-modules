package no.nav.common.health.selftest;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;

import java.util.List;
import java.util.stream.Collectors;

public class SelfTestUtils {

    public static List<SelftTestCheckResult> checkAll(List<HealthCheck> checks) {
        return checks.stream()
                .map(SelfTestUtils::performSelftTestCheck)
                .collect(Collectors.toList());
    }

    public static List<SelftTestCheckResult> checkAllParallel(List<HealthCheck> checks) {
        return checks.parallelStream()
                .map(SelfTestUtils::performSelftTestCheck)
                .collect(Collectors.toList());
    }

    public static SelfTestStatus toStatus(SelftTestCheckResult result) {
        boolean isHealthy = result.checkResult.isHealthy();
        boolean isCritical = result.metadata.isCritical();

        if (isHealthy) {
            return SelfTestStatus.OK;
        }

        return isCritical ? SelfTestStatus.ERROR : SelfTestStatus.WARNING;
    }

    public static int findHttpStatusCode(List<SelftTestCheckResult> checkResults) {
        return findHttpStatusCode(checkResults, false);
    }

    public static int findHttpStatusCode(List<SelftTestCheckResult> checkResults, boolean failOnWarning) {
        SelfTestStatus status = aggregateStatus(checkResults);

        if (failOnWarning && status == SelfTestStatus.WARNING || status == SelfTestStatus.ERROR) {
            return 500;
        }

        return 200;
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

    public static SelftTestCheckResult performSelftTestCheck(HealthCheck check) {
        long beforeCheck = System.currentTimeMillis();
        HealthCheckResult result = HealthCheckUtils.safeCheckHealth(check);
        long timeUsed = System.currentTimeMillis() - beforeCheck;

        return new SelftTestCheckResult(check.healthCheckMetadata(), result, timeUsed);
    }

}
