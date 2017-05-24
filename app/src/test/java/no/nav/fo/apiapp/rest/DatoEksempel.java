package no.nav.fo.apiapp.rest;

import javax.ws.rs.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static no.nav.apiapp.rest.DateConfiguration.DEFAULT_ZONE;

@Path("/dato")
public class DatoEksempel {


    @GET
    @Path("/dto")
    public DTO getDTO() {
        return new DTO();
    }

    @GET
    @Path("/query")
    public DTO queryDTO(@BeanParam QueryDTO queryDTO) {
        return tilDTO(queryDTO);
    }

    @POST
    @Path("/query")
    public DTO postQueryDTO(QueryDTO queryDTO) {
        return tilDTO(queryDTO);
    }


    private DTO tilDTO(QueryDTO queryDTO) {
        DTO dto = new DTO();
        if (queryDTO != null) {
            dto.localDate = queryDTO.localDate;
            dto.localDateTime = queryDTO.localDateTime;
            dto.zonedDateTime = queryDTO.zonedDateTime;
            dto.date = queryDTO.date;
            dto.optionalDate = ofNullable(queryDTO.date);
            dto.string = queryDTO.string;
        }
        return dto;
    }

    @SuppressWarnings("unused")
    public static class DTO {
        public LocalDate localDate = LocalDate.of(2017, 5, 10);
        public LocalDateTime localDateTime = localDate.atTime(1, 2, 3, 4);
        public ZonedDateTime zonedDateTime = localDateTime.atZone(DEFAULT_ZONE);
        public Date date = Date.from(zonedDateTime.toInstant());
        public boolean aBoolean = true;
        public Optional<Date> optionalDate = of(date);
        public Optional<Date> noOptionalDate = Optional.empty();
        public String string;
    }

    public static class QueryDTO {
        @QueryParam("localDate")
        public LocalDate localDate;
        @QueryParam("localDateTime")
        public LocalDateTime localDateTime;
        @QueryParam("zonedDateTime")
        public ZonedDateTime zonedDateTime;
        @QueryParam("date")
        public Date date;
        @QueryParam("string")
        public String string;
    }

}
