package no.nav.common.types.identer;

public abstract class EksternBrukerId extends Id {

    public enum Type {
        FNR, AKTOR_ID, NORSK_IDENT
    }

    EksternBrukerId(String id) {
        super(id);
    }

    public Type type() {
        if (this instanceof AktorId) {
            return Type.AKTOR_ID;
        } else if (this instanceof Fnr) {
            return Type.FNR;
        } else if (this instanceof NorskIdent) {
            return Type.NORSK_IDENT;
        }

        throw new IllegalStateException("Ukjent EksternBrukerId.Type");
    }

}
