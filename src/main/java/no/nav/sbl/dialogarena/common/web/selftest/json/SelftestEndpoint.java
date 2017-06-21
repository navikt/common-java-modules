package no.nav.sbl.dialogarena.common.web.selftest.json;

public class SelftestEndpoint {
    private String endpoint;
    private String description;
    private String errorMessage;
    private Integer result;
    private String responseTime;
    private String stacktrace;

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
}
