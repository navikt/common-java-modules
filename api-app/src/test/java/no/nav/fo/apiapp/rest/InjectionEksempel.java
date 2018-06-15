package no.nav.fo.apiapp.rest;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/inject")
public class InjectionEksempel {

    @Inject
    private javax.inject.Provider<HttpServletRequest> httpServletRequestProvider;

    @GET
    public String get(){
        return httpServletRequestProvider.get().getRequestURI();
    }

}
