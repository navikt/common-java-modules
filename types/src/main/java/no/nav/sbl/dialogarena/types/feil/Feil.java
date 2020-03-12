package no.nav.sbl.dialogarena.types.feil;

import javax.ws.rs.core.Response;

public class Feil extends RuntimeException {

    private final Type type;

    public Feil(Type type) {
        super(type.getName());
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

    public interface Type {
        String getName();
        Response.Status getStatus();
    }

}
