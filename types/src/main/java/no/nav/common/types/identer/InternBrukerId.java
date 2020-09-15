package no.nav.common.types.identer;

public abstract class InternBrukerId extends Id {

    public enum Type {
        NAV_IDENT, AZURE_AD
    }

    InternBrukerId(String id) {
        super(id);
    }

    public InternBrukerId.Type type() {
        if (this instanceof AzureObjectId) {
            return Type.AZURE_AD;
        } else if (this instanceof NavIdent) {
            return Type.NAV_IDENT;
        }

        throw new IllegalStateException("Ukjent InternBrukerId.Type");
    }

}
