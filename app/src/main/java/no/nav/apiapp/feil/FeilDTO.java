package no.nav.apiapp.feil;

public class FeilDTO {
    public final String id;
    public final Feil.Type type;
    public final String detaljer;

    public FeilDTO(String id, Feil.Type type, String detaljer) {
        this.id = id;
        this.type = type;
        this.detaljer = detaljer;
    }
}
