package no.nav.common.auth;

public class Constants {

    // Id token (FSS)
    public final static String AZURE_AD_ID_TOKEN_COOKIE_NAME = "isso-idtoken";

    // Id token (FSS)
    public final static String OPEN_AM_ID_TOKEN_COOKIE_NAME = "ID_token";

    // Id token (SBS)
    public final static String AZURE_AD_B2C_ID_TOKEN_COOKIE_NAME = "selvbetjening-idtoken";

    // Custom claim for NAV ident, brukes i V1 av Azure AD. Skal ikke brukes i V2.
    public final static String AAD_NAV_IDENT_CLAIM = "NAVident";

    public final static String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    
}
