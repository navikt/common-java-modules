package no.nav.common.oidc;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Constants {

    // Id token (FSS)
    public final static String ISSO_ID_TOKEN_COOKIE_NAME = "isso-idtoken";

    // Id token (SBS)
    public final static String ESSO_ID_TOKEN_COOKIE_NAME = "selvbetjening-idtoken";

    // Custom claim for NAV ident
    public final static String AAD_NAV_IDENT_CLAIM = "NAVident";

    public final static String OPEN_AM_DISCOVERY_URL_PROPERTY_NAME = "OPENAM_DISCOVERY_URL";
    public final static String OPEN_AM_CLIENT_ID_PROPERTY_NAME = "VEILARBLOGIN_OPENAM_CLIENT_ID";

    public final static String AAD_DISCOVERY_URL_PROPERTY_NAME = "AAD_DISCOVERY_URL";
    public final static String AAD_CLIENT_ID_PROPERTY_NAME = "VEILARBLOGIN_AAD_CLIENT_ID";

    public static String getOpenAmDiscoveryUrl() {
        return getRequiredProperty(OPEN_AM_DISCOVERY_URL_PROPERTY_NAME);
    }

    public static String getOpenAmClientId() {
        return getRequiredProperty(OPEN_AM_CLIENT_ID_PROPERTY_NAME);
    }

}
