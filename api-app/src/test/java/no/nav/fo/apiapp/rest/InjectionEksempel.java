package no.nav.fo.apiapp.rest;

import no.nav.fo.apiapp.EksempelService;
import no.nav.sbl.util.AssertUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/inject")
@Component
public class InjectionEksempel {

    @Inject
    private javax.inject.Provider<HttpServletRequest> httpServletRequestProvider;

    @Inject
    private EksempelService eksempelService;

    @Inject
    private InjectionEksempel(EksempelService eksempelService) {
        AssertUtils.assertNotNull(eksempelService);
    }

    @GET
    public String get() {
        return httpServletRequestProvider.get().getRequestURI();
    }

    @GET
    @Path("/service")
    public String getService() {
        return eksempelService.get();
    }

}
