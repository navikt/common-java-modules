package no.nav.brukerdialog.security.oidc;



import no.nav.fo.feed.common.OutInterceptor;

import javax.ws.rs.client.Invocation;

public class OidcFeedOutInterceptor implements OutInterceptor {

    private SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();

    @Override
    public void apply(Invocation.Builder builder) {
        builder.header("Authorization", "Bearer " + systemUserTokenProvider.getToken());
    }
}
