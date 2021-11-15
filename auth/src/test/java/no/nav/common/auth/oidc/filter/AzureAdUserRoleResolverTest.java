package no.nav.common.auth.oidc.filter;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.auth.context.UserRole;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class AzureAdUserRoleResolverTest {

    @Test
    public void resolve_subEqualsOid_returnsSystemUser() {
        AzureAdUserRoleResolver resolver = new AzureAdUserRoleResolver();
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet
                .Builder()
                .claim("sub", new String("test")) // Wrap with new String() to test equals() vs ==
                .claim("oid", "test")
                .build();

        assertEquals(UserRole.SYSTEM, resolver.resolve(jwtClaimsSet));
    }

    @Test
    public void resolve_subNotEqualsOid_returnsInternUser() {
        AzureAdUserRoleResolver resolver = new AzureAdUserRoleResolver();
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet
                .Builder()
                .claim("sub", "test")
                .claim("oid", "test2")
                .build();

        assertEquals(UserRole.INTERN, resolver.resolve(jwtClaimsSet));
    }

    @Test
    public void resolve_subIsNull_throwsException() {
        AzureAdUserRoleResolver resolver = new AzureAdUserRoleResolver();
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet
                .Builder()
                .claim("sub", null)
                .claim("oid", "test")
                .build();

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(jwtClaimsSet));
    }

    @Test
    public void resolve_subIsUndefined_throwsException() {
        AzureAdUserRoleResolver resolver = new AzureAdUserRoleResolver();
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet
                .Builder()
                .claim("oid", "test")
                .build();

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(jwtClaimsSet));
    }

}
