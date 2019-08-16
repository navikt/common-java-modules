package no.nav.apiapp.securitylevel;

import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Component
@Path("/")
public class SecurityLevelsService {

    @GET
    @Path("/default")
    public String def() {
        return "ok";
    }

    @GET
    @Path("/default/path")
    public String def_2() {
        return "ok";
    }

    @GET
    @Path("/level4")
    public String level4() {
        return "ok";
    }

    @GET
    @Path("/level4/path")
    public String level4_2() {
        return "ok";
    }

    @GET
    @Path("/level4/with/long/path")
    public String level4_3() {
        return "ok";
    }

    @GET
    @Path("/level2")
    public String level2() {
        return "ok";
    }

    @GET
    @Path("/level2/path")
    public String level2_2() {
        return "ok";
    }

    @GET
    @Path("/level2/with/long/path")
    public String level2_3() {
        return "ok";
    }
}
