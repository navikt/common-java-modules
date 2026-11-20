package no.nav.common.client.aktorregister;

import java.util.List;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

public interface AktorregisterClient extends AktorOppslagClient {

    /**
     * Henter alle aktørider på bruker med fnr.
     *
     * @param fnr fødselsnummer på brukeren
     * @return liste med alle aktørider som bruker har hatt
     */
    List<AktorId> hentAktorIder(Fnr fnr);
}
