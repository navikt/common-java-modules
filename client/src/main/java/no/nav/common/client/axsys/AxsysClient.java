package no.nav.common.client.axsys;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;

import java.util.List;

public interface AxsysClient {
    List<NavIdent> hentAnsatte(EnhetId enhetId);

    AxsysEnheter hentTilganger(NavIdent veileder);
}
