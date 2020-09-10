package no.nav.common.client.aktorregister;

import no.nav.common.health.HealthCheck;

import java.util.List;

public interface AktorregisterClient extends HealthCheck {

    String hentFnr(String aktorId);

    String hentAktorId(String fnr);

    List<IdentOppslag> hentFnr(List<String> aktorIdListe);

    List<IdentOppslag> hentAktorId(List<String> fnrListe);

    List<String> hentAktorIder(String fnr);
}
