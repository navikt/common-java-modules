package no.nav.brukerdialog.security.oidc;

public class OidcTokenValidatorResult {
    private final boolean isValid;
    private final String errorMessage;
    private final String subject;
    private final long expSeconds;

    private OidcTokenValidatorResult(boolean isValid, String errorMessage, String subject, long expSeconds) {
        this.isValid = isValid;
        this.errorMessage = errorMessage;
        this.subject = subject;
        this.expSeconds = expSeconds;
    }

    public static OidcTokenValidatorResult invalid(String errorMessage) {
        return new OidcTokenValidatorResult(false, errorMessage, null, 0);
    }

    public static OidcTokenValidatorResult valid(String subject, long expSeconds) {
        return new OidcTokenValidatorResult(true, null, subject, expSeconds);
    }

    public boolean isValid() {
        return isValid;
    }

    public String getErrorMessage() {
        if (isValid) {
            throw new IllegalArgumentException("Can't get error message from valid token");
        }
        return errorMessage;
    }

    public String getSubject() {
        if (!isValid) {
            throw new IllegalArgumentException("Can't get claims from an invalid token");
        }
        return subject;
    }

    public long getExpSeconds() {
        if (!isValid) {
            throw new IllegalArgumentException("Can't get claims from an invalid token");
        }
        return expSeconds;
    }
}
