package no.nav.dialogarena.smoketest;

import lombok.*;
import lombok.experimental.Accessors;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.oidc.TokenUtils;
import no.nav.brukerdialog.security.oidc.UserTokenProvider;
import no.nav.dialogarena.config.DevelopmentSecurity;

import javax.ws.rs.core.Cookie;

import static no.nav.dialogarena.config.DevelopmentSecurity.setupIntegrationTestSecurity;

@Getter
@Setter
public class SmoketestFSS {
    private String innloggetVeielder;
    private Cookie tokenCookie;

    private static final String ID_TOKEN = "ID_token";

    public SmoketestFSS(SmoketestFSSConfig config) {
        setupidcSecurity(config);
    }

    private void setupidcSecurity(SmoketestFSSConfig config) {
        setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig(config.getApplicationName()));

        UserTokenProvider userTokenProvider = new UserTokenProvider();
        OidcCredential token = userTokenProvider.getIdToken();

        tokenCookie = new Cookie(ID_TOKEN, token.getToken());
        innloggetVeielder = TokenUtils.getTokenSub(token.getToken());
    }

    @Data
    @Accessors(chain = true)
    public static class SmoketestFSSConfig {
        private final String applicationName;
    }
}
