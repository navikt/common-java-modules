package no.nav.fo.apiapp.rest;

import no.nav.apiapp.feil.Feil;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/eksempel")
public class RestEksempel {

    @GET
    public String get() {
        return "eksempel";
    }

    @GET
    @Path("/ukjent-feil")
    public String ukjentFeil() {
        throw new RuntimeException();
    }


    @GET
    @Path("/spesial-feil")
    public String spesialFeil() {
        throw new Feil(new Feil.Type() {
            @Override
            public String getName() {
                return "CustomFeil";
            }

            @Override
            public Response.Status getStatus() {
                return Response.Status.GONE;
            }
        });
    }

    @PUT
    @Path("/pipe")
    public Map<String, Object> pipeData(Map<String, Object> data) {
        return data;
    }

    @Path("/tall/{grunnTall}")
    public TallRessurs nostetRessurs(@PathParam("grunnTall") int grunnTall) {
        return new TallRessurs(grunnTall);
    }

    public static class TallRessurs {

        private final int grunnTall;

        public TallRessurs(int grunnTall) {
            this.grunnTall = grunnTall;
        }

        @GET
        @Path("/pluss/{tall}")
        public int pluss(@PathParam("tall") int tall) {
            return grunnTall + tall;
        }

        @GET
        @Path("/minus/{tall}")
        public int minus(@PathParam("tall") int tall) {
            return grunnTall - tall;
        }
    }

}
