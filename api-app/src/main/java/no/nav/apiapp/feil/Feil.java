package no.nav.apiapp.feil;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.*;
import static no.nav.apiapp.util.EnumUtils.getName;

public class Feil extends RuntimeException {

    private final Type type;

    public Feil(Type type) {
        super(getName(type));
        this.type = type;
    }

    public Feil(Type type, String feilmelding) {
        super(feilmelding);
        this.type = type;
    }

    public Feil(Type type, Throwable throwable) {
        super(throwable);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        UGYLDIG_REQUEST(BAD_REQUEST),
        UGYLDIG_HANDLING(CONFLICT),
        FINNES_IKKE(NOT_FOUND),
        VERSJONSKONFLIKT(BAD_REQUEST),
        INGEN_TILGANG(FORBIDDEN),
        UKJENT(INTERNAL_SERVER_ERROR),
        SERVICE_UNAVAILABLE(Response.Status.SERVICE_UNAVAILABLE);

        private final Response.Status status;

        Type(Response.Status status) {
            this.status = status;
        }

        public Response.Status getStatus() {
            return status;
        }
    }

}
