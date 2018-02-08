package no.nav.apiapp;

import static java.lang.System.clearProperty;
import static no.nav.apiapp.Constants.MILJO_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.STS_URL_KEY;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.SYSTEMUSER_USERNAME;

public class TestContext {

    public static void setup() {
        // cleanup av properties som ofte settes men ikke alltid fjernes
        clearProperty(STS_URL_KEY);
        clearProperty(SYSTEMUSER_USERNAME);
        clearProperty(SYSTEMUSER_PASSWORD);
        clearProperty(MILJO_PROPERTY_NAME);
    }

}
