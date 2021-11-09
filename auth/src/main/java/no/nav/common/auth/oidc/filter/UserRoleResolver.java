package no.nav.common.auth.oidc.filter;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.auth.context.UserRole;

public interface UserRoleResolver {
    UserRole resolve(JWTClaimsSet jwt);
}
