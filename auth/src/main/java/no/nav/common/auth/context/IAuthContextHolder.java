package no.nav.common.auth.context;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.common.utils.fn.UnsafeSupplier;

import java.util.Optional;

public interface IAuthContextHolder {

    void withContext(AuthContext authContext, UnsafeRunnable runnable);

    <T> T withContext(AuthContext authContext, UnsafeSupplier<T> supplier);

    @SuppressWarnings("unused")
    NavIdent requireNavIdent();

    String requireSubject();

    String requireIdTokenString();

    JWTClaimsSet requireIdTokenClaims();

    UserRole requireRole();

    AuthContext requireContext();

    Optional<String> getSubject();

    Optional<String> getIdTokenString();

    Optional<JWTClaimsSet> getIdTokenClaims();

    /**
     * Hent NAV ident for innlogget saksbehandler.
     * Sjekker først etter custom claim med NAV ident, hvis ikke så brukes subject fra tokenet.
     * Det er viktig å vite at det er kun OpenAM som har NAV ident som subject.
     * Det gjøres en filtrering på gyldig NAV ident slik at metoden ikke blir misbrukt på feil type tokens.
     *
     * @return NAV ident
     */
    Optional<NavIdent> getNavIdent();

    Optional<UserRole> getRole();

    Optional<AuthContext> getContext();

    void setContext(AuthContext authContext);

    @SuppressWarnings("unused")
    boolean erInternBruker();

    @SuppressWarnings("unused")
    boolean erSystemBruker();

    @SuppressWarnings("unused")
    boolean erEksternBruker();

    boolean harBrukerRolle(UserRole userRole);

}
