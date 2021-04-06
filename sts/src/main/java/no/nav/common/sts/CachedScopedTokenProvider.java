package no.nav.common.sts;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static no.nav.common.sts.utils.StsTokenUtils.tokenNeedsRefresh;

public class CachedScopedTokenProvider implements ScopedTokenProvider {

    private final ScopedTokenProvider scopedTokenProvider;

    private final Map<String, JWT> cachedTokens = new ConcurrentHashMap<>();

    public CachedScopedTokenProvider(ScopedTokenProvider scopedTokenProvider) {
        this.scopedTokenProvider = scopedTokenProvider;
    }

    @SneakyThrows
    @Override
    public String getToken(String scope) {
        JWT token = cachedTokens.get(scope);

        if (tokenNeedsRefresh(token)) {
            token = JWTParser.parse(scopedTokenProvider.getToken(scope));
            cachedTokens.put(scope, token);
        }

        return token.getParsedString();
    }

}
