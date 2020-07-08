package no.nav.common.health.selftest;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNull;

import java.util.List;

public class SelfTestMeterBinder implements MeterBinder {

    private final SelfTestChecks selfTestChecks;

    public SelfTestMeterBinder(SelfTestChecks selfTestChecks) {
        this.selfTestChecks = selfTestChecks;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        Gauge.builder("selftests_aggregate_result_status", this::getAggregateResult)
                .description("aggregert status for alle selftester. 0=ok, 1=kritisk feil, 2=ikke-kritisk feil")
                .register(registry);
    }

    private int getAggregateResult() {
        List<SelftTestCheckResult> selftTestCheckResults = SelfTestUtils.checkAllParallel(selfTestChecks.getSelfTestChecks());
        SelfTestStatus selfTestStatus = SelfTestUtils.aggregateStatus(selftTestCheckResults);
        return selfTestStatus.statusKode;
    }
}
