package no.nav.fo.apiapp.rest;

import no.nav.sbl.featuretoggle.unleash.UnleashService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/toggled")
public class FeatureToggledExample {

    private final UnleashService unleashService;

    @Inject
    public FeatureToggledExample(UnleashService unleashService) {
        this.unleashService = unleashService;
    }

    @GET
    public boolean isActive() {
        return unleashService.isEnabled("myFeature");
    }

}
