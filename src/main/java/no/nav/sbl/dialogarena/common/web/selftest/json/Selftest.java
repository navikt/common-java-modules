package no.nav.sbl.dialogarena.common.web.selftest.json;

import java.util.List;

public class Selftest {
    private String application;
    private String version;
    private String timestamp;
    private int aggregateResult;
    List<SelftestEndpoint> checks;

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
        return checks;
    }

    public Selftest setChecks(List<SelftestEndpoint> checks) {
        this.checks = checks;
        return this;
    }
}
