package no.nav.fo.apiapp.rest;

import javax.ws.rs.*;
import java.sql.Timestamp;
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
    public DateDTO getDTO() {
        return new DateDTO();
    }

    @GET
    @Path("/query")
    public DateDTO queryDTO(@BeanParam QueryDTO queryDTO) {
        return tilDTO(queryDTO);
    }

    @POST
    @Path("/query")
    public DateDTO postQueryDTO(QueryDTO queryDTO) {
        return tilDTO(queryDTO);
    }


    private DateDTO tilDTO(QueryDTO queryDTO) {
        DateDTO dto = new DateDTO();
        if (queryDTO != null) {
            dto.localDate = queryDTO.localDate;
            dto.localDateTime = queryDTO.localDateTime;
            dto.zonedDateTime = queryDTO.zonedDateTime;
            dto.date = queryDTO.date;
            dto.optionalDate = ofNullable(queryDTO.date);
            dto.string = queryDTO.string;
            dto.timestamp = queryDTO.timestamp;
            dto.sqlDate = queryDTO.sqlDate;
            dto.string = queryDTO.string;
        }
        return dto;
    }

    @SuppressWarnings("unused")
    public static class DateDTO {
        public LocalDate localDate = LocalDate.of(2017, 5, 10);
        public LocalDateTime localDateTime = localDate.atTime(1, 2, 3, 4);
        public ZonedDateTime zonedDateTime = localDateTime.atZone(DEFAULT_ZONE);
        public Date date = Date.from(zonedDateTime.toInstant());
        public boolean aBoolean = true;
        public Optional<Date> optionalDate = of(date);
        public Optional<Date> noOptionalDate = Optional.empty();
        public String string;
        public Timestamp timestamp = Timestamp.from(zonedDateTime.toInstant());
        public java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);
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
        @QueryParam("timestamp")
        public Timestamp timestamp;
        @QueryParam("sqlDate")
        public java.sql.Date sqlDate;
    }

}
