package no.nav.common.client.pdl;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

import java.util.List;
import java.util.Map;

public interface AktorOppslagClient extends HealthCheck {

    /**
     * Henter gjeldende fnr basert på brukers aktør id
     * @param aktorId aktør id som det blir gjort oppslag på
     * @return gjeldende fnr for bruker
     */
    Fnr hentFnr(AktorId aktorId);

    /**
     * Henter gjeldende aktør id basert på brukers fnr
     * @param fnr fnr som det blir gjort oppslag på
     * @return gjeldende aktør id for bruker
     */
    AktorId hentAktorId(Fnr fnr);

    /**
     * Henter gjeldende fnr for en liste med aktør ider fra forskjellige brukere
     * @param aktorIdListe liste med aktør ider som det blir gjort oppslag på
     * @return et map med aktør id det har blitt gjort oppslag med som key og fnr som value.
     *          Hvis oppslag på et fnr feilet så vil hverken aktør id eller fnr ligge i mappet for oppslaget.
     */
    Map<AktorId, Fnr> hentFnrBolk(List<AktorId> aktorIdListe);

    /**
     * Henter gjeldende aktør id for en liste med fnr fra forskjellige brukere
     * @param fnrListe liste med fnr som det blir gjort oppslag på
     * @return et map med fnr det har blitt gjort oppslag med som key og aktør id som value.
     *          Hvis oppslag på en aktør id feilet så vil hverken fnr eller aktør ligge i mappet for oppslaget.
     */
    Map<Fnr, AktorId> hentAktorIdBolk(List<Fnr> fnrListe);

}
