package no.nav.fo.apiapp.rest;

import javax.ws.rs.*;
import java.util.Arrays;
import java.util.List;

@Path("/enum")
public class EnumEksempel {

    @GET
    @Path("/")
    public List<EnEnum> getEnumer() {
        return Arrays.asList(EnEnum.values());
    }

    @GET
    @Path("/{ordinal}")
    public EnEnum enumForOrdinal(@PathParam("ordinal") int ordinal) {
        return EnEnum.values()[ordinal];
    }

    @GET
    @Path("/dto/{ordinal}")
    public EnumDTO dtoForOrdinal(@PathParam("ordinal") int ordinal) {
        EnumDTO dto = new EnumDTO();
        dto.enEnum = EnEnum.values()[ordinal];
        dto.enums = getEnumer();
        return dto;
    }

    @POST
    @Path("/")
    public EnumDTO pipeDto(EnumDTO dto) {
        return dto;
    }

    @PUT
    @Path("/")
    public EnEnum pipeEnum(EnEnum enEnum) {
        return enEnum;
    }

    @SuppressWarnings("unused")
    public static class EnumDTO {
        public EnEnum enEnum;
        public List<EnEnum> enums;
    }

    public enum EnEnum {
        ABC,
        DEF,
        GHI
    }

}
