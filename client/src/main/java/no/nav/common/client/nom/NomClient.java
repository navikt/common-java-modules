package no.nav.common.client.nom;

import java.util.List;
import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.NavIdent;

public interface NomClient extends HealthCheck {

    VeilederNavn finnNavn(NavIdent navIdent);

    List<VeilederNavn> finnNavn(List<NavIdent> navIdenter);
}
