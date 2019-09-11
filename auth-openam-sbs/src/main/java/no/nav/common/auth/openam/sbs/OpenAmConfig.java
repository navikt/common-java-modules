package no.nav.common.auth.openam.sbs;

import lombok.Builder;
import lombok.Value;
import no.nav.util.sbl.EnvironmentUtils;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class OpenAmConfig {

    public static final String OPENAM_RESTURL = "openam.restUrl";
    public static final String OPENAM_RESTURL_ENVIRONMENT_VARIABLE = "OPENAM_RESTURL";

    public String restUrl;
    public List<String> additionalAttributes;
    @Builder.Default
    public OpenAMEventListener openAMEventListener = new OpenAMEventListener.DefaultOpenAMEventListener();

    public static OpenAmConfig fromSystemProperties() {
        return OpenAmConfig.builder()
                .restUrl(EnvironmentUtils.getRequiredProperty(OPENAM_RESTURL, OPENAM_RESTURL_ENVIRONMENT_VARIABLE))
                .build();
    }

}
