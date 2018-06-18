package no.nav.sbl.featuretoggle.unleash;


import lombok.Builder;
import lombok.Value;
import no.finn.unleash.util.UnleashScheduledExecutor;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireApplicationName;

@Builder
@Value
public class UnleashServiceConfig {
    public static final String UNLEASH_API_URL_PROPERTY_NAME = "UNLEASH_API_URL";

    public String applicationName;
    public String unleashApiUrl;
    public UnleashScheduledExecutor unleashScheduledExecutor;

    public static UnleashServiceConfig resolveFromEnvironment(){
        return UnleashServiceConfig.builder()
                .applicationName(requireApplicationName())
                .unleashApiUrl(getRequiredProperty(UNLEASH_API_URL_PROPERTY_NAME))
                .build();
    }

}
