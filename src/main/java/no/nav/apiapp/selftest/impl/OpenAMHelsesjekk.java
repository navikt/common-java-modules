package no.nav.apiapp.selftest.impl;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.modig.security.filter.OpenAMService;

import static no.nav.apiapp.config.Konfigurator.OPENAM_RESTURL;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;


public class OpenAMHelsesjekk implements Helsesjekk {

    private final OpenAMService openAMService = new OpenAMService();
    private final HelsesjekkMetadata helsesjekkMetadata;

    public OpenAMHelsesjekk() {
        helsesjekkMetadata = new HelsesjekkMetadata(
                "openam",
                getRequiredProperty(OPENAM_RESTURL),
                "Sjekker et ugyldig token mot openAM",
                true
        );
    }

    @Override
    public void helsesjekk() throws Throwable {
        if (openAMService.isTokenValid(OpenAMHelsesjekk.class.getName())) {
            throw new IllegalStateException();
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return helsesjekkMetadata;
    }

}
