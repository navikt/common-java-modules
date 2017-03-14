package no.nav.sbl.dialogarena.common.abac.pep.exception;


public class PepException extends Exception {

    public PepException(String message) {
        super(message);
    }

    public PepException(String s, Exception e) {
        super(s, e);
    }

    public PepException(Exception e) {
        super(e);
    }
}
