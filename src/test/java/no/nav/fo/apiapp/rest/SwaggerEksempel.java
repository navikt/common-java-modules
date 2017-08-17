package no.nav.fo.apiapp.rest;

import io.swagger.annotations.*;

import javax.ws.rs.*;
import java.time.ZonedDateTime;
import java.util.Date;

@Api( // NB: ikke påkrevd
        value = "Eksempel",
        description = "deeeesc"
)
@Path("/eksempel/swagger")
public class SwaggerEksempel {


    @ApiOperation(
            value = "eksempel-value",
            code = 1234,
            notes = "eksempel-notes"
    )
    @ApiResponse(code = 345, message = "asdfaf")
    @GET
    public String medEksplisittDokumentasjon() {
        return "test";
    }

    @HEAD
    @ApiImplicitParam(name = "implisitt-parameter2")
    public String medDefaultDokumentasjon() {
        return "test";
    }

    @POST
    public RiktObjekt rikRespons(RiktObjekt riktObjekt) {
        return new RiktObjekt();
    }

    public static class RiktObjekt {
        public Date dato;
        public ZonedDateTime zonedDateTime;
        public String string;
        public boolean bool;
        public long tall;
        public EnEnum enEnum;
    }

    public enum  EnEnum {
        EN,TO,TRE;
    }

}
