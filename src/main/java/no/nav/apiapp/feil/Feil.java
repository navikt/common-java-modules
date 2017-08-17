package no.nav.apiapp.feil;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.*;

public class Feil extends RuntimeException {

    private final Type type;

    public Feil(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        UGYLDIG_REQUEST(BAD_REQUEST),
        FINNES_IKKE(NOT_FOUND),
        VERSJONSKONFLIKT(BAD_REQUEST),
        INGEN_TILGANG(FORBIDDEN),
        UKJENT(INTERNAL_SERVER_ERROR);

        private final Response.Status status;

        Type(Response.Status status) {
            this.status = status;
        }

        public Response.Status getStatus() {
            return status;
        }
    }

}
