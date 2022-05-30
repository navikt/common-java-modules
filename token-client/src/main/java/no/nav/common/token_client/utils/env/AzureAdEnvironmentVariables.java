package no.nav.common.token_client.utils.env;

/**
 * Environment variables that are injected into NAIS applications configured with Azure AD.
 * See: https://doc.nais.io/security/auth/azure-ad/#runtime-variables-credentials
 */
public class AzureAdEnvironmentVariables {

    public final static String AZURE_OPENID_CONFIG_TOKEN_ENDPOINT = "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT";

    public final static String AZURE_APP_CLIENT_ID = "AZURE_APP_CLIENT_ID";

    public final static String AZURE_APP_TENANT_ID = "AZURE_APP_TENANT_ID";

    public final static String AZURE_APP_CLIENT_SECRET = "AZURE_APP_CLIENT_SECRET";

    public final static String AZURE_APP_PRE_AUTHORIZED_APPS = "AZURE_APP_PRE_AUTHORIZED_APPS";

    public final static String AZURE_APP_WELL_KNOWN_URL = "AZURE_APP_WELL_KNOWN_URL";

    public final static String AZURE_APP_JWKS = "AZURE_APP_JWKS";

    public final static String AZURE_APP_JWK = "AZURE_APP_JWK";

    public final static String AZURE_OPENID_CONFIG_JWKS_URI = "AZURE_OPENID_CONFIG_JWKS_URI";

    public final static String AZURE_OPENID_CONFIG_ISSUER = "AZURE_OPENID_CONFIG_ISSUER";

    public final static String AZURE_APP_CERTIFICATE_KEY_ID = "AZURE_APP_CERTIFICATE_KEY_ID";

    public final static String AZURE_APP_PASSWORD_KEY_ID = "AZURE_APP_PASSWORD_KEY_ID";



    public final static String AZURE_APP_NEXT_JWK = "AZURE_APP_NEXT_JWK";

    public final static String AZURE_APP_NEXT_CLIENT_SECRET = "AZURE_APP_NEXT_CLIENT_SECRET";

    public final static String AZURE_APP_NEXT_CERTIFICATE_KEY_ID = "AZURE_APP_NEXT_CERTIFICATE_KEY_ID";

    public final static String AZURE_APP_NEXT_PASSWORD_KEY_ID = "AZURE_APP_NEXT_PASSWORD_KEY_ID";

}
