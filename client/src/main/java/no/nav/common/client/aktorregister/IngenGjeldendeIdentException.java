package no.nav.common.client.aktorregister;

public class IngenGjeldendeIdentException extends IllegalStateException {
    public IngenGjeldendeIdentException() {
        super("Fant ikke gjeldende ident");
    }
}
