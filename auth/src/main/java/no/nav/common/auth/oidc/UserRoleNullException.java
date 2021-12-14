package no.nav.common.auth.oidc;

public class UserRoleNullException extends IllegalArgumentException{
    public UserRoleNullException() {
        super("User role kan ikke være null");
    }
}
