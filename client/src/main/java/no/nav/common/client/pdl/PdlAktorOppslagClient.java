package no.nav.common.client.pdl;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

public interface PdlAktorOppslagClient extends HealthCheck {

    Fnr hentFnr(AktorId aktorId);

    AktorId hentAktorId(Fnr fnr);

}
