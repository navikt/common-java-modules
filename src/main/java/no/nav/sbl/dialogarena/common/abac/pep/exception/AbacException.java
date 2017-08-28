package no.nav.sbl.dialogarena.common.abac.pep.exception;

public class AbacException extends RuntimeException {
    public AbacException(String message) {
        super(message);
    }

    public AbacException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
