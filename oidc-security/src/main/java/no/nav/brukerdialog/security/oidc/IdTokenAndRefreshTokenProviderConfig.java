package no.nav.brukerdialog.security.oidc;

import lombok.Builder;
import lombok.Value;
import no.nav.brukerdialog.security.Constants;

@Value
@Builder
public class IdTokenAndRefreshTokenProviderConfig {

    public final String issoHostUrl;
    public final String issoRpUserUsername;
    public final String issoRpUserPassword;

    public static IdTokenAndRefreshTokenProviderConfig from(SystemUserTokenProviderConfig systemUserTokenProviderConfig) {
        return IdTokenAndRefreshTokenProviderConfig.builder()
                .issoHostUrl(systemUserTokenProviderConfig.issoHostUrl)
                .issoRpUserUsername(systemUserTokenProviderConfig.issoRpUserUsername)
                .issoRpUserPassword(systemUserTokenProviderConfig.issoRpUserPassword)
                .build();
    }

    public static IdTokenAndRefreshTokenProviderConfig resolveFromSystemProperties() {
        return builder()
                .issoHostUrl(Constants.getIssoHostUrl())
                .issoRpUserUsername(Constants.getIssoRpUserUsername())
                .issoRpUserPassword(Constants.getIssoRpUserPassword())
                .build();
    }

}
