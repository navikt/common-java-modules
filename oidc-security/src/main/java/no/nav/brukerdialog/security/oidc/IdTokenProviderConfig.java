package no.nav.brukerdialog.security.oidc;

import lombok.Builder;
import lombok.Value;
import no.nav.brukerdialog.security.Constants;
import no.nav.brukerdialog.security.oidc.provider.IssoOidcProviderConfig;


@Value
@Builder
public class IdTokenProviderConfig {

    public String issoHostUrl;
    public String issoRpUserPassword;

    public static IdTokenProviderConfig resolveFromSystemProperties() {
        return IdTokenProviderConfig.builder()
                .issoHostUrl(Constants.getIssoHostUrl())
                .issoRpUserPassword(Constants.getIssoRpUserPassword())
                .build();
    }

    public static IdTokenProviderConfig from(IssoOidcProviderConfig issoOidcProviderConfig) {
        return IdTokenProviderConfig.builder()
                .issoHostUrl(issoOidcProviderConfig.issoHostUrl)
                .issoRpUserPassword(issoOidcProviderConfig.issoRpUserPassword)
                .build();
    }

}
