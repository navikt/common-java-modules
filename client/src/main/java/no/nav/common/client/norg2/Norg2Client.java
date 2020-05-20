package no.nav.common.client.norg2;

import no.nav.common.health.HealthCheck;

import java.util.List;

public interface Norg2Client extends HealthCheck {

    List<Enhet> alleAktiveEnheter();

    Enhet hentEnhet(String enhetId);

    /**
     * Henter enheten som tilhører et geografisk område
     * @param geografiskOmrade ISO 3166 koden til området (f.eks NAV Frogner tilhører 030105)
     * @return NAV enhet som tilhører det geografiske området
     */
    Enhet hentTilhorendeEnhet(String geografiskOmrade);

}
