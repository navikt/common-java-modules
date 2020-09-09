package no.nav.common.client.aktorregister;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

import java.util.List;

public interface AktorregisterClient extends HealthCheck {

    Fnr hentFnr(AktorId aktorId);

    AktorId hentAktorId(Fnr fnr);

    List<IdentOppslag> hentFnr(List<AktorId> aktorIdListe);

    List<IdentOppslag> hentAktorId(List<Fnr> fnrListe);

}
