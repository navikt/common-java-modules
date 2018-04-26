package no.nav.modig.core.context;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

public class AuthenticationLevelCredential implements Destroyable{

    private int authenticationLevel;
    private boolean destroyed;

    public AuthenticationLevelCredential(int authenticationLevel) {
        this.authenticationLevel = authenticationLevel;
    }

    public int getAuthenticationLevel() {
        return authenticationLevel;
    }

    @Override
    public void destroy() throws DestroyFailedException {
        authenticationLevel = -1;
        destroyed = true;

    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("[")
                .append(destroyed ? "destroyed" : authenticationLevel)
                .append("]");
        return sb.toString();
    }
}
