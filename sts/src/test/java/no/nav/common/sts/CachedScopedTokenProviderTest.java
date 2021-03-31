package no.nav.common.sts;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.Test;

import java.util.Date;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CachedScopedTokenProviderTest {

    @Test
    public void should_cache_tokens_with_same_scope() {
        String accessToken = new PlainJWT(
                new JWTClaimsSet.Builder().expirationTime(new Date(System.currentTimeMillis() + 100_000)).build()
        ).serialize();

        ScopedTokenProvider provider = mock(ScopedTokenProvider.class);

        when(provider.getToken(anyString())).thenReturn(accessToken);

        CachedScopedTokenProvider cachedProvider = new CachedScopedTokenProvider(provider);

        String scope1 = "SCOPE_1";
        String scope2 = "SCOPE_2";

        cachedProvider.getToken(scope1);
        cachedProvider.getToken(scope1);

        cachedProvider.getToken(scope2);

        verify(provider, times(1)).getToken(eq(scope1));
        verify(provider, times(1)).getToken(eq(scope2));
    }

    @Test
    public void should_not_cache_expired_token() {
        String accessToken = new PlainJWT(
                new JWTClaimsSet.Builder().expirationTime(new Date(System.currentTimeMillis() + 59_000)).build()
        ).serialize();

        ScopedTokenProvider provider = mock(ScopedTokenProvider.class);

        when(provider.getToken(anyString())).thenReturn(accessToken);

        CachedScopedTokenProvider cachedProvider = new CachedScopedTokenProvider(provider);

        String scope = "SCOPE";

        cachedProvider.getToken(scope);
        cachedProvider.getToken(scope);

        verify(provider, times(2)).getToken(eq(scope));
    }

}
