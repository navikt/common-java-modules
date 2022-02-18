package no.nav.common.auth.context;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.auth.utils.IdentUtils;
import no.nav.common.auth.utils.TokenUtils;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.common.utils.fn.UnsafeSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;
import static no.nav.common.auth.Constants.ID_PORTEN_PID_CLAIM;

/**
 * Holds the authentication context such as the role and ID token of a logged in user
 */
public interface AuthContextHolder {

    final class InternalLogger {
        private final static Logger log = LoggerFactory.getLogger(AuthContextHolder.class);
    }

    void withContext(AuthContext authContext, UnsafeRunnable runnable);

    <T> T withContext(AuthContext authContext, UnsafeSupplier<T> supplier);

    @SuppressWarnings("unused")
    default NavIdent requireNavIdent() {
        return getNavIdent().orElseThrow(() -> new IllegalStateException("NAV Ident is missing from AuthContext"));
    }

    default String requireSubject() {
        return getSubject().orElseThrow(() -> new IllegalStateException("Subject is missing from AuthContext"));
    }

    default String requireIdTokenString() {
        return getIdTokenString().orElseThrow(() -> new IllegalStateException("ID token is missing from AuthContext"));
    }

    default JWTClaimsSet requireIdTokenClaims() {
        return getIdTokenClaims().orElseThrow(() -> new IllegalStateException("ID token is missing from AuthContext"));
    }

    default UserRole requireRole() {
        return getRole().orElseThrow(() -> new IllegalStateException("User role is missing from AuthContext"));
    }

    default AuthContext requireContext() {
        return getContext().orElseThrow(() -> new IllegalStateException("AuthContext is missing"));
    }

    default Optional<String> getSubject() {
        return getIdTokenClaims().map(JWTClaimsSet::getSubject);
    }

    default Optional<String> getIdTokenString() {
        return getContext()
                .map(AuthContext::getIdToken)
                .map(token -> ofNullable(token.getParsedString()).orElse(token.serialize()));
    }

    default Optional<JWTClaimsSet> getIdTokenClaims() {
        return getContext()
                .map(AuthContext::getIdToken)
                .map(TokenUtils::getClaimsSet);
    }

    /**
     * Hent NAV ident for innlogget saksbehandler.
     * Sjekker først etter custom claim med NAV ident, hvis ikke så brukes subject fra tokenet.
     * Det er viktig å vite at det er kun OpenAM som har NAV ident som subject.
     * Det gjøres en filtrering på gyldig NAV ident slik at metoden ikke blir misbrukt på feil type tokens.
     *
     * @return NAV ident
     */
    default Optional<NavIdent> getNavIdent() {
        return getIdTokenClaims()
                .flatMap(claims -> getStringClaim(claims, AAD_NAV_IDENT_CLAIM))
                .or(this::getSubject)
                .map(NavIdent::of)
                .filter(navIdent -> {
                    boolean erGyldig = IdentUtils.erGydligNavIdent(navIdent.get());

                    if (!erGyldig) {
                        InternalLogger.log.error("NAV ident er ugyldig: " + navIdent);
                    }

                    return erGyldig;
                });
    }

    /**
     * @return fnr fra pid-claim for ekstern bruker, NAV ident fra NAVident-claim for intern bruker, med fallback til
     * sub-claim.
     */
    default Optional<String> getUid() {
        if (erEksternBruker()) {
            return getIdTokenClaims()
                    .flatMap(claims -> getStringClaim(claims, ID_PORTEN_PID_CLAIM))
                    .or(this::getSubject);
        } else if (erInternBruker()) {
            return getNavIdent().map(NavIdent::get).or(this::getSubject);
        }
        return getSubject();
    }

    default Optional<String> getStringClaim(JWTClaimsSet claims, String claimName) {
        try {
            return ofNullable(claims.getStringClaim(claimName));
        } catch (Exception e) {
            InternalLogger.log.warn(claimName + " was not a string");
            return empty();
        }
    }

    default Optional<UserRole> getRole() {
        return getContext().map(AuthContext::getRole);
    }

    Optional<AuthContext> getContext();

    void setContext(AuthContext authContext);

    @SuppressWarnings("unused")
    default boolean erInternBruker() {
        return harBrukerRolle(UserRole.INTERN);
    }

    @SuppressWarnings("unused")
    default boolean erSystemBruker() {
        return harBrukerRolle(UserRole.SYSTEM);
    }

    @SuppressWarnings("unused")
    default boolean erEksternBruker() {
        return harBrukerRolle(UserRole.EKSTERN);
    }

    default boolean harBrukerRolle(UserRole userRole) {
        return getRole()
                .map(role -> role == userRole)
                .orElse(false);
    }

}
