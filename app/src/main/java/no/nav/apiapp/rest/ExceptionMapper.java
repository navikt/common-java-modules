package no.nav.apiapp.rest;

import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.FeilDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static no.nav.apiapp.feil.Feil.Type.UKJENT;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMapper.class);

    public static final String MILJO_PROPERTY_NAME = "environment.class";
    private static final Set<String> MILJO_MED_DETALJER = new HashSet<>(asList("t", "u", "q"));

    @Inject
    javax.inject.Provider<HttpServletRequest> servletRequestProvider;

    @Override
    public Response toResponse(Throwable exception) {
        Feil.Type type = getType(exception);
        Response.Status status = getStatus(exception);
        FeilDTO feil = new FeilDTO(UUID.randomUUID().toString(), type, visDetaljer() ? getStackTrace(exception) : null);
        LOGGER.error("{} - {} - {}", servletRequestProvider.get().getRequestURI(), status, feil);
        LOGGER.error(exception.getMessage(), exception);
        return Response
                .status(status)
                .entity(feil)
                // TODO big-ip-header! Sjekke om denne fikser hvis applikasjonen er nede!
//                .header("")
                .build();
    }


    private boolean visDetaljer() {
        return ofNullable(System.getProperty(MILJO_PROPERTY_NAME)).map(MILJO_MED_DETALJER::contains).orElse(false);
    }

    private Feil.Type getType(Throwable throwable) {
        if (throwable instanceof Feil) {
            return ((Feil) throwable).getType();
        } else {
            return UKJENT;
        }
    }

    private Response.Status getStatus(Throwable throwable) {
        if (throwable instanceof WebApplicationException) {
            return fromStatusCode(((WebApplicationException) throwable).getResponse().getStatus());
        } else {
            return getType(throwable).getStatus();
        }
    }

}
