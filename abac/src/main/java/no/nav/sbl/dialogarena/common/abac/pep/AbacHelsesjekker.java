package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig;
import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static no.nav.sbl.rest.RestUtils.withClient;


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
            Ping.PingMetadata metadata = new Ping.PingMetadata(endpointUrl, "ABAC tilgangskontroll - ping", true);
            try {
                pepClient.ping();
                return Ping.lyktes(metadata);
            } catch (Throwable e) {
                return Ping.feilet(metadata, e);
            }
        }
    }

    @Component
    public static class SelfTest implements Pingable {

        private final String endpointUrl;

        public SelfTest(AbacServiceConfig abacServiceConfig) {
            this.endpointUrl = abacServiceConfig.getEndpointUrl();
        }

        @Override
        public Ping ping() {
            URI build = getSelfTestURI();
            Ping.PingMetadata metadata = new Ping.PingMetadata(build.toASCIIString(), "ABAC tilgangskontroll - selftest", true);
            try {
                return withClient(client -> {
                    Response response = client.target(build).request().get();
                    int responseStatus = response.getStatus();
                    if (responseStatus != 200 || response.readEntity(String.class).toLowerCase().contains("error")) {
                        return Ping.feilet(metadata, String.format("%s - %s", responseStatus, response.getStatusInfo()));
                    } else {
                        return Ping.lyktes(metadata);
                    }
                });
            } catch (Throwable e) {
                return Ping.feilet(metadata, e);
            }
        }

        private URI getSelfTestURI() {
            // TODO kan dette eksponeres som fasit-ressurs?
            return UriBuilder.fromUri(endpointUrl)
                    .replacePath("/asm-pdp-monitor/selftest")
                    .build();
        }

    }

}
