package no.nav.common.auth.openam.sbs;

import lombok.Builder;
import lombok.Value;
import no.nav.sbl.util.EnvironmentUtils;

import java.util.Collections;
import java.util.List;

import static no.nav.common.auth.openam.sbs.OpenAMUserInfoService.PARAMETER_UID;

@Value
@Builder(toBuilder = true)
public class OpenAmConfig {

    public static final String OPENAM_RESTURL = "openam.restUrl";
    public static final String OPENAM_RESTURL_ENVIRONMENT_VARIABLE = "OPENAM_RESTURL";

    public String restUrl;
    public List<String> additionalAttributes;

    public static OpenAmConfig fromSystemProperties() {
        return OpenAmConfig.builder()
                .restUrl(EnvironmentUtils.getRequiredProperty(OPENAM_RESTURL, OPENAM_RESTURL_ENVIRONMENT_VARIABLE))
                .build();
    }

}
