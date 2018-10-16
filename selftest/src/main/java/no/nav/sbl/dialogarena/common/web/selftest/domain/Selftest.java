package no.nav.sbl.dialogarena.common.web.selftest.domain;

import lombok.Builder;
import lombok.Value;
import no.nav.sbl.dialogarena.common.web.selftest.SelfTestStatus;

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
