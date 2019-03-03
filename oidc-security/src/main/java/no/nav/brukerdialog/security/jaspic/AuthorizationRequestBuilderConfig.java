package no.nav.brukerdialog.security.jaspic;

import lombok.Builder;
import lombok.Value;
import no.nav.brukerdialog.security.Constants;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.SecureRandom;

import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.brukerdialog.security.Constants.getIssoRpUserUsername;
import static no.nav.brukerdialog.security.Constants.getOidcRedirectUrl;

@Value
@Builder
public class AuthorizationRequestBuilderConfig {

    private String issoHostUrl;
    private String clientId;
    private String redirectUrl;

    public static AuthorizationRequestBuilderConfig resolveFromSystemProperties() {
        return AuthorizationRequestBuilderConfig.builder()
                .issoHostUrl(Constants.getIssoHostUrl())
                .clientId(getIssoRpUserUsername())
                .redirectUrl(getOidcRedirectUrl())
                .build();
    }

}