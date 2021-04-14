package no.nav.common.client.nom;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.NavIdent;

@Data
@Accessors(chain = true)
public class VeilederNavn {
    NavIdent navIdent;
    String fornavn;
    String etternavn;
    String mellomnavn;
}