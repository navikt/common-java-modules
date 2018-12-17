package no.nav.apiapp.config;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IssoConfig {

    public final String username;
    public final String password;

    public final String issoHostUrl;
    public final String issoRpUserUsername;
    public final String issoRpUserPassword;
    public final String oidcRedirectUrl;
    public final String issoJwksUrl;
    public final String issoExpectedTokenIssuer;
    public final String isAliveUrl;

}
