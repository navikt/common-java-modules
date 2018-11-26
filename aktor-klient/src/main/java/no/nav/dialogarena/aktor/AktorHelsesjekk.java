package no.nav.dialogarena.aktor;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.dialogarena.aktor.AktorConfig.getAktorEndpointUrl;

@Component
public class AktorHelsesjekk implements Helsesjekk {

    @Inject
    private AktoerV2 aktoerV2;

    @Override
    public void helsesjekk() {
        aktoerV2.ping();
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        String aktoerUrl = getAktorEndpointUrl();
        return new HelsesjekkMetadata(
                "aktoer",
                "virksomhet:Aktoer_v2 via " + aktoerUrl,
                "Ping av aktoer_v2 (konvertere mellom aktorId og Fnr).",
                true
        );
    }

}
