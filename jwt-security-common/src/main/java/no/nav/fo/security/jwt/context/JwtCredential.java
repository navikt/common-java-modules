package no.nav.fo.security.jwt.context;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

public class JwtCredential implements Destroyable {
    private boolean destroyed;
    private String jwt;

    public JwtCredential(String jwt) {
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
            return "JwtCredential[destroyed]";
        }
        return "JwtCredential[" + this.jwt + "]";
    }

}
