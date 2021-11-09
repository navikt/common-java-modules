package no.nav.common.auth.oidc.filter;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.auth.context.UserRole;

/**
 * UserRole resolver for Azure AD (Skal ikke brukes med andre OIDC providers).
 * Resolveren sjekker om tokenet er på vegne av en bruker (INTERN), eller om tokenet er et system-til-system (SYSTEM) token og returnerer riktig rolle.
 */
public class AzureAdUserRoleResolver implements UserRoleResolver {

    /*
        Dersom "sub" er lik "oid" så er tokenet anskaffet via client credentials flow, altså et token uten brukerinnvolvering.
        Om ikke, så er tokenet anskaffet på vegne av en bruker der "oid" er brukerens globale ID i Azure AD.
        Kilde: https://security.labs.nais.io/pages/tokenvalidering/azure-ad.html#skille-mellom-tokens-med-og-uten-brukerkontekst
    */

    @Override
    public UserRole resolve(JWTClaimsSet jwtClaimsSet) {
        var sub = jwtClaimsSet.getClaim("sub");
        var oid = jwtClaimsSet.getClaim("oid");

        if (sub == null || oid == null) {
            throw new IllegalArgumentException("Kunne ikke resolve UserRole. sub eller oid i token er null");
        }

        return sub == oid
                ? UserRole.SYSTEM
                : UserRole.INTERN;
    }

}
