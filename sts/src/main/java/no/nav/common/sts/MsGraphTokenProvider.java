package no.nav.common.sts;

/**
 * Provides tokens for accessing Microsoft Graph API without a user in context
 */
public class MsGraphTokenProvider implements TokenProvider {

    private final static String MS_GRAPH_SCOPE = "https://graph.microsoft.com/.default";

    private final ScopedTokenProvider scopedTokenProvider;

    public MsGraphTokenProvider(ScopedTokenProvider scopedTokenProvider) {
        this.scopedTokenProvider = scopedTokenProvider;
    }

    @Override
    public String getToken() {
        return scopedTokenProvider.getToken(MS_GRAPH_SCOPE);
    }

}
