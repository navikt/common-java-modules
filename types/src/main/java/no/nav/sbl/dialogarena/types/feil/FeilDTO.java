package no.nav.sbl.dialogarena.types.feil;

public class FeilDTO {
    public final String id;
    public final String type;
    public final Detaljer detaljer;

    public FeilDTO(String id, String type, Detaljer detaljer) {
        this.id = id;
        this.type = type;
        this.detaljer = detaljer;
    }

    public static class Detaljer {
        public final String detaljertType;
        public final String feilMelding;
        public final String stackTrace;

        public Detaljer(String detaljertType, String feilMelding, String stackTrace) {
            this.detaljertType = detaljertType;
            this.feilMelding = feilMelding;
            this.stackTrace = stackTrace;
        }

        @Override
        public String toString() {
            return "Detaljer{" +
                    "detaljertType='" + detaljertType + '\'' +
                    ", feilMelding='" + feilMelding + '\'' +
                    ", stackTrace='" + stackTrace + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "FeilDTO{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", detaljer=" + detaljer +
                '}';
    }
}
