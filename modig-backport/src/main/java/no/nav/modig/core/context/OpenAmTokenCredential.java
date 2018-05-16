package no.nav.modig.core.context;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

public class OpenAmTokenCredential implements Destroyable {
    private String openAmToken;
    private boolean destroyed;


    public OpenAmTokenCredential(String openAmToken) {
        this.openAmToken = openAmToken;
    }

    public String getOpenAmToken() {
        return openAmToken;
    }

    @Override
    public void destroy() throws DestroyFailedException {
        openAmToken = null;
        destroyed = true;

    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("[")
                .append(destroyed ? "destroyed" : openAmToken)
                .append("]");
        return sb.toString();
    }
}
