package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig;
import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.stereotype.Component;

public class AbacHelsesjekker {

    @Component
    public static class Ping implements Pingable {

        private final PepImpl pepClient;
        private final String endpointUrl;

        public Ping(PepImpl pepClient, AbacServiceConfig abacServiceConfig) {
            this.pepClient = pepClient;
            this.endpointUrl = abacServiceConfig.getEndpointUrl();
        }

        @Override
        public Ping ping() {
            Ping.PingMetadata metadata = new Ping.PingMetadata("abac_ping", endpointUrl, "ABAC tilgangskontroll - ping", true);
            try {
                pepClient.ping();
                return Ping.lyktes(metadata);
            } catch (Throwable e) {
                return Ping.feilet(metadata, e);
            }
        }
    }
}
