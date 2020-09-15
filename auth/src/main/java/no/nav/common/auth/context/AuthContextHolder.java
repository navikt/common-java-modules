package no.nav.common.auth.context;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.utils.IdentUtils;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.common.utils.fn.UnsafeSupplier;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;

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

    @SuppressWarnings("unused")
    public static NavIdent requireNavIdent() {
        return getNavIdent().orElseThrow(() -> new IllegalStateException("NAV Ident is missing from AuthContext"));
    }

    public static String requireSubject() {
        return getSubject().orElseThrow(() -> new IllegalStateException("Subject is missing from AuthContext"));
    }

    public static String requireIdTokenString() {
        return getIdTokenString().orElseThrow(() -> new IllegalStateException("ID token is missing from AuthContext"));
    }

    @SuppressWarnings("unused")
    public static JWTClaimsSet requireIdTokenClaims() {
        return getIdTokenClaims().orElseThrow(() -> new IllegalStateException("ID token is missing from AuthContext"));
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
                .map(token -> ofNullable(token.getParsedString()).orElse(token.serialize()));
    }

    public static Optional<JWTClaimsSet> getIdTokenClaims() {
        return getContext()
                .map(AuthContext::getIdToken)
                .map(AuthContextHolder::getClaimsSet);
    }

    /**
     * Hent NAV ident for innlogget saksbehandler.
     * Sjekker først etter custom claim med NAV ident, hvis ikke så brukes subject fra tokenet.
     * Det er viktig å vite at det er kun OpenAM som har NAV ident som subject.
     * Det gjøres en filtrering på gyldig NAV ident slik at metoden ikke blir misbrukt på feil type tokens.
     * @return NAV ident
     */
    public static Optional<NavIdent> getNavIdent() {
        return getIdTokenClaims()
                .flatMap(claims -> {
                    try {
                        return ofNullable(claims.getStringClaim(AAD_NAV_IDENT_CLAIM))
                                .map(NavIdent::of)
                                .or(() -> getSubject().map(NavIdent::of));
                    } catch (Exception e) {
                        log.warn(AAD_NAV_IDENT_CLAIM + " was not a string");
                        return empty();
                    }
                }).filter(navIdent -> {
                    boolean erGyldig = IdentUtils.erGydligNavIdent(navIdent.get());

                    if (!erGyldig) {
                        log.error("NAV ident er ugyldig: " + navIdent);
                    }

                    return erGyldig;
                });
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

    @SuppressWarnings("unused")
    public static boolean erInternBruker() {
        return harBrukerRolle(UserRole.INTERN);
    }

    @SuppressWarnings("unused")
    public static boolean erSystemBruker() {
        return harBrukerRolle(UserRole.SYSTEM);
    }

    @SuppressWarnings("unused")
    public static boolean erEksternBruker() {
        return harBrukerRolle(UserRole.EKSTERN);
    }

    public static boolean harBrukerRolle(UserRole userRole) {
        return getRole()
                .map(role -> role == userRole)
                .orElse(false);
    }

    @SneakyThrows
    private static JWTClaimsSet getClaimsSet(JWT jwt) {
        return jwt.getJWTClaimsSet();
    }

}
