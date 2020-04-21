package no.nav.common.abac.exception;

public class AbacException extends RuntimeException {
    public AbacException(String message) {
        super(message);
    }

    public AbacException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
