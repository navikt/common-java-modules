package no.nav.common.sts;

/**
 * Provides tokens that uses OAuth 2.0 scope
 * See: https://oauth.net/2/scope
 */
public interface ScopedTokenProvider {

    String getToken(String scope);

}
