package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.stereotype.Component;

import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacService.getEndpointUrl;


@Component
public class AbacHelsesjekk implements Pingable {

    private final PepImpl pepClient;

    public AbacHelsesjekk(PepImpl pepClient) {
        this.pepClient = pepClient;
    }

    @Override
    public Ping ping() {
        Ping.PingMetadata metadata = new Ping.PingMetadata(getEndpointUrl(), "ABAC tilgangskontroll", true);
        try {
            pepClient.ping();
            return Ping.lyktes(metadata);
        } catch (Exception e) {
            return Ping.feilet(metadata, e);
        }
    }

}
