package no.nav.log;

import java.util.function.Supplier;

public final class LogFilterConfig {

    private final Supplier<Boolean> exposeErrorDetails;
    private final String serverName;

    LogFilterConfig(Supplier<Boolean> exposeErrorDetails, String serverName) {
        this.exposeErrorDetails = exposeErrorDetails;
        this.serverName = serverName;
    }

    public static LogFilterConfigBuilder builder() {
        return new LogFilterConfigBuilder();
    }

    public Supplier<Boolean> getExposeErrorDetails() {
        return this.exposeErrorDetails;
    }

    public String getServerName() {
        return this.serverName;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof LogFilterConfig)) return false;
        final LogFilterConfig other = (LogFilterConfig) o;
        final Object this$exposeErrorDetails = this.getExposeErrorDetails();
        final Object other$exposeErrorDetails = other.getExposeErrorDetails();
        if (this$exposeErrorDetails == null ? other$exposeErrorDetails != null : !this$exposeErrorDetails.equals(other$exposeErrorDetails))
            return false;
        final Object this$serverName = this.getServerName();
        final Object other$serverName = other.getServerName();
        if (this$serverName == null ? other$serverName != null : !this$serverName.equals(other$serverName))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $exposeErrorDetails = this.getExposeErrorDetails();
        result = result * PRIME + ($exposeErrorDetails == null ? 43 : $exposeErrorDetails.hashCode());
        final Object $serverName = this.getServerName();
        result = result * PRIME + ($serverName == null ? 43 : $serverName.hashCode());
        return result;
    }

    public String toString() {
        return "LogFilterConfig(exposeErrorDetails=" + this.getExposeErrorDetails() + ", serverName=" + this.getServerName() + ")";
    }

    public static class LogFilterConfigBuilder {
        private Supplier<Boolean> exposeErrorDetails;
        private String serverName;

        LogFilterConfigBuilder() {
        }

        public LogFilterConfig.LogFilterConfigBuilder exposeErrorDetails(Supplier<Boolean> exposeErrorDetails) {
            this.exposeErrorDetails = exposeErrorDetails;
            return this;
        }

        public LogFilterConfig.LogFilterConfigBuilder serverName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        public LogFilterConfig build() {
            return new LogFilterConfig(exposeErrorDetails, serverName);
        }

        public String toString() {
            return "LogFilterConfig.LogFilterConfigBuilder(exposeErrorDetails=" + this.exposeErrorDetails + ", serverName=" + this.serverName + ")";
        }
    }
}
