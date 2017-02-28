package no.nav.brukerdialog.security.domain;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

public class OidcCredential implements Destroyable {
    private boolean destroyed;
    private String jwt;

    public OidcCredential(String jwt) {
        this.jwt = jwt;
    }

    public String getToken() {
        return jwt;
    }

    @Override
    public void destroy() throws DestroyFailedException {
        jwt = null;
        destroyed = true;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public String toString() {
        if (destroyed) {
            return "OidcCredential[destroyed]";
        }
        return "OidcCredential[" + this.jwt + "]";
    }

}
