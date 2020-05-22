package no.nav.apiapp.feil;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.*;

public enum FeilType implements Feil.Type {
    UGYLDIG_REQUEST(BAD_REQUEST),
    UGYLDIG_HANDLING(CONFLICT),
    FINNES_IKKE(NOT_FOUND),
    INGEN_TILGANG(FORBIDDEN),
    UKJENT(INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(Response.Status.SERVICE_UNAVAILABLE);

    private final Response.Status status;

    FeilType(Response.Status status) {
        this.status = status;
    }


    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public Response.Status getStatus() {
        return status;
    }
}
