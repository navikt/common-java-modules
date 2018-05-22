package no.nav.common.auth.openam.sbs;

import lombok.Value;
import no.nav.sbl.util.EnvironmentUtils;

@Value
public class OpenAmConfig {

    public static final String OPENAM_RESTURL = "openam.restUrl";
    public static final String OPENAM_RESTURL_ENVIRONMENT_VARIABLE = "OPENAM_RESTURL";

    public String restUrl;

    public static OpenAmConfig fromSystemProperties() {
        return new OpenAmConfig(EnvironmentUtils.getRequiredProperty(OPENAM_RESTURL, OPENAM_RESTURL_ENVIRONMENT_VARIABLE));
    }
}
