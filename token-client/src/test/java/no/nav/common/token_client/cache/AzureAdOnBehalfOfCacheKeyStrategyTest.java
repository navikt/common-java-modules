package no.nav.common.token_client.cache;

import no.nav.common.token_client.test_utils.TokenCreator;
import org.junit.jupiter.api.Test;

import static no.nav.common.token_client.cache.AzureAdOnBehalfOfCacheKeyStrategy.ASSERTION_HASH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AzureAdOnBehalfOfCacheKeyStrategyTest {

    @Test
    public void shouldCreateStableKeyWithoutExposingInput() {
        String token = TokenCreator.instance().createToken("sensitive-subject");
        String key = ASSERTION_HASH.createCacheKey("sensitive-client", "sensitive-endpoint", "sensitive-scope", token);

        assertEquals(key, ASSERTION_HASH.createCacheKey(
                "sensitive-client", "sensitive-endpoint", "sensitive-scope", token));
        assertTrue(key.matches("azure-obo:v1:[0-9a-f]{64}"));
    }

    @Test
    public void shouldIsolateClientEndpointScopeAndIncomingToken() {
        String token = TokenCreator.instance().createToken("user");
        String otherToken = TokenCreator.instance().createToken("user");
        String key = ASSERTION_HASH.createCacheKey("client", "endpoint", "scope", token);

        assertNotEquals(key, ASSERTION_HASH.createCacheKey("other-client", "endpoint", "scope", token));
        assertNotEquals(key, ASSERTION_HASH.createCacheKey("client", "other-endpoint", "scope", token));
        assertNotEquals(key, ASSERTION_HASH.createCacheKey("client", "endpoint", "other-scope", token));
        assertNotEquals(key, ASSERTION_HASH.createCacheKey("client", "endpoint", "scope", otherToken));
    }
}
