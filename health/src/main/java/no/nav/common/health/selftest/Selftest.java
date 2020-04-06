package no.nav.common.health.selftest;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

import static java.util.Optional.ofNullable;

@Value
@Builder
public class Selftest {

    private String application;
    private String version;
    private String timestamp;
    private SelfTestStatus aggregateResult;
    private List<SelftestResult> checks;

    public List<SelftestResult> getChecks() {
        return ofNullable(checks).orElseGet(Collections::emptyList);
    }

}
