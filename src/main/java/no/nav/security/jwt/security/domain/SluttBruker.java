package no.nav.security.jwt.security.domain;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import java.security.Principal;

public final class SluttBruker implements Principal, Destroyable {

    private String uid;
    private IdentType identType;
    private boolean destroyed;

    public SluttBruker(String uid, IdentType identType) {
        this.uid = uid;
        this.identType = identType;
    }

    public static SluttBruker internBruker(String uid) {
        return new SluttBruker(uid, IdentType.InternBruker);
    }

    public IdentType getIdentType() {
        return identType;
    }

    @Override
    public String getName() {
        return uid;
    }

    @Override
    public void destroy() throws DestroyFailedException {
        uid = null;
        destroyed = true;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                "identType=" + identType + ", " +
                "uid=" + (destroyed ? "destroyed" : uid) +
                "]";
    }
}
