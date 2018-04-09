package no.nav.fo.apiapp.security;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

@Path("/session")
public class KreverSesjon {

    @GET
    public String getSesjonsId(@Context HttpServletRequest httpSession){
        return httpSession.getSession().getId();
    }

}
