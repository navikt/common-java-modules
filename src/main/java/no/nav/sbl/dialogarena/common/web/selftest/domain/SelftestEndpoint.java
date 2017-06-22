package no.nav.sbl.dialogarena.common.web.selftest.domain;

import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet.STATUS_OK;

public class SelftestEndpoint {
    private String endpoint;
    private String description;
    private String errorMessage;
    private Integer result;
    private String responseTime;
    private String stacktrace;
    private boolean critical;

    public String getEndpoint() {
        return endpoint;
    }

    public SelftestEndpoint setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public SelftestEndpoint setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public SelftestEndpoint setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public int getResult() {
        return result;
    }

    public SelftestEndpoint setResult(int result) {
        this.result = result;
        return this;
    }

    public String getResponseTime() {
        return responseTime;
    }

    public SelftestEndpoint setResponseTime(String responseTime) {
        this.responseTime = responseTime;
        return this;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public SelftestEndpoint setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
        return this;
    }

    public boolean isCritical() {
        return this.critical;
    }

    public SelftestEndpoint setCritical(boolean critical) {
        this.critical = critical;
        return this;
    }

    public boolean harFeil() {
        return this.getResult() != STATUS_OK;
    }
}
