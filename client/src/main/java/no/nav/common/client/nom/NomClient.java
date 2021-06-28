package no.nav.common.client.nom;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.NavIdent;

import java.util.List;

public interface NomClient extends HealthCheck {

    VeilederVisningsnavn finnVisningsnavn(NavIdent navIdent);

    List<VeilederVisningsnavn> finnVisningsnavn(List<NavIdent> navIdenter);

}
