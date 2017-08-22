package no.nav.apiapp.feil;

import lombok.ToString;

@ToString
public class FeilDTO {
    public final String id;
    public final Feil.Type type;
    public final Detaljer detaljer;

    public FeilDTO(String id, Feil.Type type, Detaljer detaljer) {
        this.id = id;
        this.type = type;
        this.detaljer = detaljer;
    }

    @ToString
    public static class Detaljer {
        public final String detaljertType;
        public final String feilMelding;
        public final String stackTrace;

        public Detaljer(String detaljertType, String feilMelding, String stackTrace) {
            this.detaljertType = detaljertType;
            this.feilMelding = feilMelding;
            this.stackTrace = stackTrace;
        }
    }

}
