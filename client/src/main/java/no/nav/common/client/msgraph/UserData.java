package no.nav.common.client.msgraph;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserData {
    String givenName; // Ola
    String surname;   // Nordmann
    String displayName; // Nordmann, Ola
    String mail; // ola.nordmann@nav.no
    String onPremisesSamAccountName; // Hvis brukeren er ansastt hos NAV så blir dette NAV Ident (Z1234567)
    String id; // 1234abcd-1234-1234-abcd-1234abcd
}

