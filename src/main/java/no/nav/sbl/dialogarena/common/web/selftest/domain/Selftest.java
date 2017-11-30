package no.nav.sbl.dialogarena.common.web.selftest.domain;

import java.util.Collections;
import java.util.List;

import static java.util.Optional.ofNullable;

public class Selftest {
    private String application;
    private String version;
    private String timestamp;
    private int aggregateResult;
    private List<SelftestEndpoint> checks;

    public String getApplication() {
        return application;
    }

    public Selftest setApplication(String application) {
        this.application = application;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Selftest setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Selftest setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public int getAggregateResult() {
        return aggregateResult;
    }

    public Selftest setAggregateResult(int aggregateResult) {
        this.aggregateResult = aggregateResult;
        return this;
    }

    public List<SelftestEndpoint> getChecks() {
        return ofNullable(checks).orElseGet(Collections::emptyList);
    }

    public Selftest setChecks(List<SelftestEndpoint> checks) {
        this.checks = checks;
        return this;
    }
}
