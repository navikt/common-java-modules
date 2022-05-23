package no.nav.common.client.norg2;

import no.nav.common.health.HealthCheck;

import java.util.List;

public interface Norg2Client extends HealthCheck {

    List<Enhet> alleAktiveEnheter();

    Enhet hentEnhet(String enhetId);

    /**
     * Henter enheten som tilhører et geografisk område
     * @param geografiskOmrade Geografisk identifikator, kommune eller bydel, for NAV kontoret (f.eks NAV Frogner tilhører 030105)
     * @param diskresjonskode Diskresjonskode på saker et NAV kontor kan behandle.
     * @param skjermet Hvorvidt NAV kontoret skal behandle saker for et skjermet ansatt
     * @return NAV enhet som tilhører det geografiske området
     */
    Enhet hentTilhorendeEnhet(String geografiskOmrade, Diskresjonskode diskresjonskode, boolean skjermet);

    enum Diskresjonskode {
        SPFO, SPSF, ANY
    }
}
