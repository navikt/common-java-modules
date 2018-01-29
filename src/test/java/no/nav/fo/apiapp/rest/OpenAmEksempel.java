package no.nav.fo.apiapp.rest;

import no.nav.modig.core.context.SubjectHandler;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("openam")
public class OpenAmEksempel {

    @GET
    public String getIdent() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

}
