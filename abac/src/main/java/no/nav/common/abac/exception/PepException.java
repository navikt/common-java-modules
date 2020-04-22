package no.nav.common.abac.exception;


public class PepException extends RuntimeException {

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
