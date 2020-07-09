package no.nav.common.health.selftest;

import java.util.List;

public class SelfTestChecks {

    private final List<SelfTestCheck> selfTestChecks;

    public SelfTestChecks(List<SelfTestCheck> selfTestChecks) {
        this.selfTestChecks = selfTestChecks;
    }

    public List<SelfTestCheck> getSelfTestChecks() {
        return selfTestChecks;
    }
}
