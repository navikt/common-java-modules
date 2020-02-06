package no.nav.fo.apiapp.rest;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import java.time.ZonedDateTime;
import java.util.Date;

@Api( // NB: ikke påkrevd
        value = "Eksempel",
        description = "deeeesc"
)
@Path("/eksempel/swagger")
@Slf4j
public class SwaggerEksempel {


    @ApiOperation(
            value = "eksempel-value",
            code = 1234,
            notes = "eksempel-notes"
    )
    @ApiResponse(code = 345, message = "asdfaf")
    @GET
    public String medEksplisittDokumentasjon(@HeaderParam("CUSTOM_HEADER") String customHeader) {
        log.info(customHeader);
        return "test";
    }

    @HEAD
    @ApiImplicitParams(@ApiImplicitParam(name = "implisitt-parameter", paramType = "header", type = "string"))
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

    public enum EnEnum {
        EN, TO, TRE;
    }

}
