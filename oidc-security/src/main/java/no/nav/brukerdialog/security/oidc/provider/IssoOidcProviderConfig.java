package no.nav.brukerdialog.security.oidc.provider;

import lombok.Builder;
import lombok.Value;
import no.nav.brukerdialog.security.Constants;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProviderConfig;


@Value
@Builder
public class IssoOidcProviderConfig {

    public String issoJwksUrl;
    public String issoHostUrl;
    public String issoRpUserPassword;
    public String issoExpectedTokenIssuer;

    public static IssoOidcProviderConfig resolveFromSystemProperties() {
        return IssoOidcProviderConfig.builder()
                .issoJwksUrl(Constants.getIssoJwksUrl())
                .issoExpectedTokenIssuer(Constants.getIssoExpectedTokenIssuer())
                .issoHostUrl(Constants.getIssoHostUrl())
                .issoRpUserPassword(Constants.getIssoRpUserPassword())
                .build();
    }

    public static IssoOidcProviderConfig from(SystemUserTokenProviderConfig systemUserTokenProviderConfig) {
        return IssoOidcProviderConfig.builder()
                .issoJwksUrl(systemUserTokenProviderConfig.issoJwksUrl)
                .issoHostUrl(systemUserTokenProviderConfig.issoHostUrl)
                .issoRpUserPassword(systemUserTokenProviderConfig.issoRpUserPassword)
                .issoExpectedTokenIssuer(systemUserTokenProviderConfig.issoExpectedTokenIssuer)
                .build();
    }

}
