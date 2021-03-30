package no.nav.common.client.axsys;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;

import java.util.List;

public interface AxsysClient extends HealthCheck {
    List<NavIdent> hentAnsatte(EnhetId enhetId);
    List<AxsysEnhet> hentTilganger(NavIdent navIdent);
}
