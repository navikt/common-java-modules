package no.nav.apiapp;

import java.util.stream.Stream;

import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.util.EnvironmentUtils.*;

public class TestContext {

    public static void setup() {
        // cleanup av properties som ofte settes men ikke alltid fjernes
        Stream.of(
                STS_URL_KEY,
                SYSTEMUSER_USERNAME,
                SYSTEMUSER_PASSWORD,
                FASIT_ENVIRONMENT_NAME_PROPERTY_NAME,
                FASIT_ENVIRONMENT_NAME_PROPERTY_NAME_SKYA,
                APP_NAME_PROPERTY_NAME,
                APP_NAME_PROPERTY_NAME_SKYA,
                APP_VERSION_PROPERTY_NAME,
                APP_VERSION_PROPERTY_NAME_SKYA
        ).forEach(System::clearProperty);
    }

}
