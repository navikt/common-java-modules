package no.nav.brukerdialog.security.oidc;

import lombok.Builder;
import lombok.Value;
import no.nav.brukerdialog.security.Constants;
import no.nav.sbl.util.EnvironmentUtils;

import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_USERNAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.resolveSrvUserPropertyName;
import static no.nav.sbl.util.EnvironmentUtils.resolverSrvPasswordPropertyName;

@Builder
@Value
public class SystemUserTokenProviderConfig {

    public final String srvUsername;
    public final String srvPassword;
    public final String issoHostUrl;
    public final String issoRpUserUsername;
    public final String issoRpUserPassword;
    public final String oidcRedirectUrl;
    public final String issoJwksUrl;
    public final String issoExpectedTokenIssuer;

    public static SystemUserTokenProviderConfig resolveFromSystemProperties() {
        return SystemUserTokenProviderConfig.builder()
                .issoHostUrl(Constants.getIssoHostUrl())
                .issoRpUserUsername(Constants.getIssoRpUserUsername())
                .issoRpUserPassword(Constants.getIssoRpUserPassword())
                .issoJwksUrl(Constants.getIssoJwksUrl())
                .issoExpectedTokenIssuer(Constants.getIssoExpectedTokenIssuer())
                .oidcRedirectUrl(Constants.getOidcRedirectUrl())
                .srvUsername(getRequiredProperty(SYSTEMUSER_USERNAME, resolveSrvUserPropertyName()))
                .srvPassword(getRequiredProperty(SYSTEMUSER_PASSWORD, resolverSrvPasswordPropertyName()))
                .build();
    }

}
