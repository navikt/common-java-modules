package no.nav.common.auth;

public class Constants {

    // Id token (FSS)
    public static final String AZURE_AD_ID_TOKEN_COOKIE_NAME = "isso-idtoken";

    // Id token (FSS)
    public static final String OPEN_AM_ID_TOKEN_COOKIE_NAME = "ID_token";

    // Id token (SBS)
    public static final String AZURE_AD_B2C_ID_TOKEN_COOKIE_NAME = "selvbetjening-idtoken";

    // Custom claim for NAV ident, brukes i V1 av Azure AD. Skal ikke brukes i V2.
    public static final String AAD_NAV_IDENT_CLAIM = "NAVident";
    // OID clam fra azure skal brukes i V2
    public static final String AZURE_OID_CLAIM = "oid";

    // Custom claim for fnr fra ID-porten
    public static final String ID_PORTEN_PID_CLAIM = "pid";

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
}
