package no.nav.common.proxy;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Duration;

@Value
@Builder
public class ProxyServletConfig {


    @NotEmpty
    public String id;
    @NotEmpty
    public String contextPath;
    @NotEmpty
    public String baseUrl;

    @NotEmpty
    @Builder.Default
    public String pingPath = "/api/ping";

    @NotNull
    @Builder.Default
    public Duration readTimeout = Duration.ofSeconds(-15);

}
