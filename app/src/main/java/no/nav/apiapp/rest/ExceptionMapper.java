package no.nav.apiapp.rest;

import no.nav.apiapp.feil.FeilDTO;
import no.nav.apiapp.feil.FeilMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.fromStatusCode;

@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Throwable> {

    public static final String ESCAPE_REDIRECT_HEADER = "Escape-5xx-Redirect";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMapper.class);

    public static final String MILJO_PROPERTY_NAME = "environment.class";

    @Inject
    javax.inject.Provider<HttpServletRequest> servletRequestProvider;

    @Override
    public Response toResponse(Throwable exception) {
        FeilDTO feil = FeilMapper.somFeilDTO(exception);
        Response.Status status = getStatus(exception);
        LOGGER.error("{} - {} - {}", servletRequestProvider.get().getRequestURI(), status, feil);
        LOGGER.error(exception.getMessage(), exception);
        return Response
                .status(status)
                .entity(feil)
                // TODO big-ip-header! Sjekke om denne fikser hvis applikasjonen er nede!
                .header(ESCAPE_REDIRECT_HEADER, "true")
                .build();
    }

    private Response.Status getStatus(Throwable throwable) {
        if (throwable instanceof WebApplicationException) {
            return fromStatusCode(((WebApplicationException) throwable).getResponse().getStatus());
        } else {
            return FeilMapper.getType(throwable).getStatus();
        }
    }

}
