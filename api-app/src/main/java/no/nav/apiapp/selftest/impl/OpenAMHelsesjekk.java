package no.nav.apiapp.selftest.impl;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.common.auth.openam.sbs.OpenAMUserInfoService;
import no.nav.common.auth.openam.sbs.OpenAmConfig;

public class OpenAMHelsesjekk implements Helsesjekk {

    private static final String DUMMY_SUBJECT = OpenAMHelsesjekk.class.getName();

    private final OpenAMUserInfoService openAMUserInfoService;
    private final HelsesjekkMetadata helsesjekkMetadata;

    public OpenAMHelsesjekk(OpenAmConfig openAmConfig) {
        openAMUserInfoService = new OpenAMUserInfoService(openAmConfig);
        helsesjekkMetadata = new HelsesjekkMetadata(
                "openam-info",
                openAMUserInfoService.getUrl(DUMMY_SUBJECT),
                "Henter brukerinfo for ugyldig subject",
                true
        );
    }

    @Override
    public void helsesjekk() throws Throwable {
        int status = openAMUserInfoService.requestUserAttributes(DUMMY_SUBJECT).getStatus();
        if (status != 401) {
            throw new IllegalStateException(String.format("HTTP status %s != 401", status));
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return helsesjekkMetadata;
    }

}
