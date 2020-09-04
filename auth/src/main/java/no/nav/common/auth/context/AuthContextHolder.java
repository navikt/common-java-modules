package no.nav.common.auth.context;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.common.utils.fn.UnsafeSupplier;

import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Holds the authentication context such as the role and ID token of a logged in user
 */
@Slf4j
public class AuthContextHolder {

    private static final ThreadLocal<AuthContext> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void withContext(AuthContext authContext, UnsafeRunnable runnable) {
        AuthContext previousContext = CONTEXT_HOLDER.get();
        try {
            CONTEXT_HOLDER.set(authContext);
            runnable.run();
        } finally {
            CONTEXT_HOLDER.set(previousContext);
        }
    }

    public static <T> T withContext(AuthContext authContext, UnsafeSupplier<T> supplier) {
        AuthContext previousContext = CONTEXT_HOLDER.get();
        try {
            CONTEXT_HOLDER.set(authContext);
            return supplier.get();
        } finally {
            CONTEXT_HOLDER.set(previousContext);
        }
    }

    public static String requireSubject() {
        return getSubject().orElseThrow(() -> new IllegalStateException("Subject is missing from AuthContext"));
    }

    public static String requireIdTokenString() {
        return getIdTokenString().orElseThrow(() -> new IllegalStateException("ID token is missing from AuthContext"));
    }

    public static JWTClaimsSet requireIdTokenClaims() {
        return getIdTokenClaims().orElseThrow(() -> new IllegalStateException("ID token is missing from AuthContext"));
    }

    public static String requireAccessToken() {
        return getAccessToken().orElseThrow(() -> new IllegalStateException("Access token is missing from AuthContext"));
    }

    public static JWT requireParsedAccessToken() {
        return getParsedAccessToken().orElseThrow(() -> new IllegalStateException("Access token is missing from AuthContext"));
    }

    public static UserRole requireRole() {
        return getRole().orElseThrow(() -> new IllegalStateException("User role is missing from AuthContext"));
    }

    public static AuthContext requireContext() {
        return getContext().orElseThrow(() -> new IllegalStateException("AuthContext is missing"));
    }


    public static Optional<String> getSubject() {
        return getIdTokenClaims().map(JWTClaimsSet::getSubject);
    }

    public static Optional<String> getIdTokenString() {
        return getContext()
                .map(AuthContext::getIdToken)
                .map(JWT::getParsedString);
    }

    public static Optional<JWTClaimsSet> getIdTokenClaims() {
        return getContext()
                .map(AuthContext::getIdToken)
                .map(AuthContextHolder::getClaimsSet);
    }

    public static Optional<String> getAccessToken() {
        return ofNullable(CONTEXT_HOLDER.get().getAccessToken());
    }

    public static Optional<JWT> getParsedAccessToken() {
        return getContext()
                .map(AuthContext::getAccessToken)
                .map(AuthContextHolder::parseToken);
    }

    public static Optional<UserRole> getRole() {
        return getContext().map(AuthContext::getRole);
    }

    public static Optional<AuthContext> getContext() {
        return ofNullable(CONTEXT_HOLDER.get());
    }

    public static void setContext(AuthContext authContext) {
        CONTEXT_HOLDER.set(authContext);
    }

    @SneakyThrows
    private static JWTClaimsSet getClaimsSet(JWT jwt) {
        return jwt.getJWTClaimsSet();
    }

    @SneakyThrows
    private static JWT parseToken(String tokenStr) {
        return JWTParser.parse(tokenStr);
    }

}
