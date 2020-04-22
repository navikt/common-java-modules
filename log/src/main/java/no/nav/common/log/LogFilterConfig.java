package no.nav.common.log;

import lombok.Builder;
import lombok.Value;

import java.util.function.Supplier;

@Value
@Builder
public class LogFilterConfig {

    private Supplier<Boolean> exposeErrorDetails;
    private String serverName;

}
