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
public class AuthContextHolder implements IAuthContextHolder {

    private static AuthContextHolder instance;

    private static final ThreadLocal<AuthContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private AuthContextHolder() {}

    public static IAuthContextHolder instance() {
        if (instance == null) {
            instance = new AuthContextHolder();
        }

        return instance;
    }

    @Override
    public void withContext(AuthContext authContext, UnsafeRunnable runnable) {
        AuthContext previousContext = CONTEXT_HOLDER.get();
        try {
            CONTEXT_HOLDER.set(authContext);
            runnable.run();
        } finally {
            CONTEXT_HOLDER.set(previousContext);
        }
    }

    @Override
    public <T> T withContext(AuthContext authContext, UnsafeSupplier<T> supplier) {
        AuthContext previousContext = CONTEXT_HOLDER.get();
        try {
            CONTEXT_HOLDER.set(authContext);
            return supplier.get();
        } finally {
            CONTEXT_HOLDER.set(previousContext);
        }
    }

    @Override
    public NavIdent requireNavIdent() {
        return getNavIdent().orElseThrow(() -> new IllegalStateException("NAV Ident is missing from AuthContext"));
    }

    @Override
    public String requireSubject() {
        return getSubject().orElseThrow(() -> new IllegalStateException("Subject is missing from AuthContext"));
    }

    @Override
    public String requireIdTokenString() {
        return getIdTokenString().orElseThrow(() -> new IllegalStateException("ID token is missing from AuthContext"));
    }

    @Override
    public JWTClaimsSet requireIdTokenClaims() {
        return getIdTokenClaims().orElseThrow(() -> new IllegalStateException("ID token is missing from AuthContext"));
    }

    @Override
    public UserRole requireRole() {
        return getRole().orElseThrow(() -> new IllegalStateException("User role is missing from AuthContext"));
    }

    @Override
    public AuthContext requireContext() {
        return getContext().orElseThrow(() -> new IllegalStateException("AuthContext is missing"));
    }

    @Override
    public Optional<String> getSubject() {
        return getIdTokenClaims().map(JWTClaimsSet::getSubject);
    }

    @Override
    public Optional<String> getIdTokenString() {
        return getContext()
                .map(AuthContext::getIdToken)
                .map(token -> ofNullable(token.getParsedString()).orElse(token.serialize()));
    }

    @Override
    public Optional<JWTClaimsSet> getIdTokenClaims() {
        return getContext()
                .map(AuthContext::getIdToken)
                .map(AuthContextHolder::getClaimsSet);
    }

    @Override
    public Optional<NavIdent> getNavIdent() {
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

    @Override
    public Optional<UserRole> getRole() {
        return getContext().map(AuthContext::getRole);
    }

    @Override
    public Optional<AuthContext> getContext() {
        return ofNullable(CONTEXT_HOLDER.get());
    }

    @Override
    public void setContext(AuthContext authContext) {
        CONTEXT_HOLDER.set(authContext);
    }

    @Override
    public boolean erInternBruker() {
        return harBrukerRolle(UserRole.INTERN);
    }

    @Override
    public boolean erSystemBruker() {
        return harBrukerRolle(UserRole.SYSTEM);
    }

    @Override
    public boolean erEksternBruker() {
        return harBrukerRolle(UserRole.EKSTERN);
    }

    @Override
    public boolean harBrukerRolle(UserRole userRole) {
        return getRole()
                .map(role -> role == userRole)
                .orElse(false);
    }

    @SneakyThrows
    private static JWTClaimsSet getClaimsSet(JWT jwt) {
        return jwt.getJWTClaimsSet();
    }

}
