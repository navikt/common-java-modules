package no.nav.dialogarena.smoketest;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.oidc.OidcTokenUtils;
import no.nav.brukerdialog.security.oidc.UserTokenProvider;

import javax.ws.rs.core.Cookie;

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
        UserTokenProvider userTokenProvider = new UserTokenProvider();
        OidcCredential token = userTokenProvider.getIdToken();

        tokenCookie = new Cookie(ID_TOKEN, token.getToken());
        innloggetVeielder = OidcTokenUtils.getTokenSub(token.getToken());
    }

    @Data
    @Accessors(chain = true)
    public static class SmoketestFSSConfig {
        private final String applicationName;
    }
}
