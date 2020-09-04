package no.nav.common.auth;

public class Constants {

    // Id token (FSS)
    public final static String AZURE_AD_ID_TOKEN_COOKIE_NAME = "isso-idtoken";

    // Access token (FSS)
    public static final String AZURE_AD_ACCESS_TOKEN_COOKIE_NAME = "isso-accesstoken";

    // Id token (FSS)
    public static final String OPEN_AM_ID_TOKEN_COOKIE_NAME = "ID_token";

    // Id token (SBS)
    public final static String AZURE_AD_B2C_ID_TOKEN_COOKIE_NAME = "selvbetjening-idtoken";

    // Custom claim for NAV ident, brukes i V1 av Azure AD. Skal ikke brukes i V2.
    @Deprecated
    public final static String AAD_NAV_IDENT_CLAIM = "NAVident";

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    
}
