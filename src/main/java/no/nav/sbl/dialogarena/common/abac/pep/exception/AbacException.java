package no.nav.sbl.dialogarena.common.abac.pep.exception;

import java.io.IOException;

public class AbacException extends Exception {
    public AbacException(String message) {
        super(message);
    }

    public AbacException(String message, IOException exception) {
        super(message, exception);
    }
}
