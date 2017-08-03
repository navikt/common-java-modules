package no.nav.apiapp.rest;

import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.FeilDTO;
import no.nav.apiapp.feil.FeilMapper;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static no.nav.apiapp.feil.FeilMapper.somFeilDTO;

@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Throwable> {

    public static final String ESCAPE_REDIRECT_HEADER = "Escape-5xx-Redirect";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMapper.class);

    @Inject
    javax.inject.Provider<HttpServletRequest> servletRequestProvider;

    @Override
    public Response toResponse(Throwable exception) {
        return toResponse(
                exception,
                getStatus(exception),
                somFeilDTO(exception)
        );
    }

    public Response toResponse(Throwable exception, Feil.Type type) {
        return toResponse(
                exception,
                type.getStatus(),
                FeilMapper.somFeilDTO(exception, type)
        );
    }

    private Response toResponse(Throwable exception, Response.Status status, FeilDTO feil) {
        String path = servletRequestProvider.get().getRequestURI();
        LOGGER.error("{} - {} - {}", path, status, feil);
        LOGGER.error(exception.getMessage(), exception);
        logToMetrics(status, path, feil);
        return Response
                .status(status)
                .entity(feil)
                // TODO big-ip-header! Sjekke om denne fikser hvis applikasjonen er nede!
                .header(ESCAPE_REDIRECT_HEADER, "true")
                .build();
    }

    private void logToMetrics(Response.Status status, String path, FeilDTO feilDTO) {
        MetricsFactory.createEvent("rest-api-error")
                .addFieldToReport("httpStatus", status.getStatusCode())
                .addFieldToReport("path", path)
                .addFieldToReport("errorId", feilDTO.id)
                .addFieldToReport("errorType", feilDTO.type)
                .setFailed()
                .report();
    }

    private Response.Status getStatus(Throwable throwable) {
        if (throwable instanceof WebApplicationException) {
            return fromStatusCode(((WebApplicationException) throwable).getResponse().getStatus());
        } else {
            return FeilMapper.getType(throwable).getStatus();
        }
    }

}
