package no.nav.common.auth.oidc.filter;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.auth.context.UserRole;

import java.util.Optional;

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
        // Skal ikke lenger bruke oid == sub for å sjekke om token er m2m
        // https://docs.nais.io/auth/entra-id/reference/?h=idtyp#claims
        var isMachineToMachineToken = Optional
            .ofNullable(jwtClaimsSet.getClaim("idtyp"))
            .map(value -> value.equals("app"))
            .orElse(false);
        return isMachineToMachineToken
                ? UserRole.SYSTEM
                : UserRole.INTERN;
    }

}
