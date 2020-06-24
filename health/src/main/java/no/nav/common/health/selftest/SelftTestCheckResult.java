package no.nav.common.health.selftest;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.common.health.HealthCheckResult;

@Data
@AllArgsConstructor
public class SelftTestCheckResult {

    HealthCheckMetadata metadata;

    HealthCheckResult checkResult;

    long timeUsed;

}
