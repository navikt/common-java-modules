package no.nav.common.featuretoggle.unleash;


import lombok.Builder;
import lombok.Value;
import no.finn.unleash.util.UnleashConfig;

import java.util.function.Supplier;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.common.utils.EnvironmentUtils.requireApplicationName;

@Builder
@Value
public class UnleashServiceConfig {
    public static final String UNLEASH_API_URL_PROPERTY_NAME = "UNLEASH_API_URL";

    public String applicationName;
    public String unleashApiUrl;
    public Supplier<UnleashConfig.Builder> unleashBuilderFactory;

    public static UnleashServiceConfig resolveFromEnvironment(){
        return UnleashServiceConfig.builder()
                .applicationName(requireApplicationName())
                .unleashApiUrl(getRequiredProperty(UNLEASH_API_URL_PROPERTY_NAME))
                .build();
    }

}
