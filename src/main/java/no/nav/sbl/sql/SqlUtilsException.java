package no.nav.sbl.sql;

public class SqlUtilsException extends RuntimeException {
    public SqlUtilsException(String message) {
        super(message);
    }

    public SqlUtilsException(Throwable cause) {
        super(cause);
    }
}
