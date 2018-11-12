package no.nav.fo.apiapp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

@Path("/server")
public class ServerEksempel {

    @GET
    public Response largeResponse() {
        return Response.ok()
                .header("LARGE_HEADER", string(15))
                .entity(string(31))
                .build();
    }

    public static String string(int sizeKB) {
        return IntStream.range(0, sizeKB * 1000)
                .mapToObj(i -> "x")
                .collect(joining(""));
    }

}
