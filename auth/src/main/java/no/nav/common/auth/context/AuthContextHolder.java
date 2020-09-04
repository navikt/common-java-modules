package no.nav.common.auth.context;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;

import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * Holds the authentication context such as the role and ID token of a logged in user
 */
public class AuthContextHolder {

    private static final ThreadLocal<AuthContext> AUTH_CONTEXT = new ThreadLocal<>();

    public static void withContext(AuthContext authContext, Runnable runnable) {
        AuthContext previousContext = AUTH_CONTEXT.get();
        try {
            AUTH_CONTEXT.set(authContext);
            runnable.run();
        } finally {
            AUTH_CONTEXT.set(previousContext);
        }
    }

    public static <T> T withContext(AuthContext authContext, Supplier<T> supplier) {
        AuthContext previousContext = AUTH_CONTEXT.get();
        try {
            AUTH_CONTEXT.set(authContext);
            return supplier.get();
        } finally {
            AUTH_CONTEXT.set(previousContext);
        }
    }

    public static String getSubject() {
        return getIdTokenClaims().getSubject();
    }

    public static String getIdTokenString() {
        return AUTH_CONTEXT.get().getIdToken().serialize();
    }

    @SneakyThrows
    public static JWTClaimsSet getIdTokenClaims() {
        return AUTH_CONTEXT.get().getIdToken().getJWTClaimsSet();
    }

    public static Optional<String> getAccessToken() {
        return ofNullable(AUTH_CONTEXT.get().getAccessToken());
    }

    public static Optional<JWT> getParsedAccessToken() {
        return ofNullable(AUTH_CONTEXT.get().getAccessToken()).map(AuthContextHolder::unsafeParse);
    }

    public static UserRole getRole() {
        return AUTH_CONTEXT.get().getRole();
    }

    public static AuthContext getContext() {
        return AUTH_CONTEXT.get();
    }

    public static void setContext(AuthContext authContext) {
        AUTH_CONTEXT.set(authContext);
    }

    @SneakyThrows
    private static JWT unsafeParse(String tokenStr) {
        return JWTParser.parse(tokenStr);
    }

}
