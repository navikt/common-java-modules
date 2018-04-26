package no.nav.modig.core.domain;

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

	public static SluttBruker eksternBruker(String uid){
		return new SluttBruker(uid, IdentType.EksternBruker);
	}

	public static SluttBruker internBruker(String uid){
		return new SluttBruker(uid, IdentType.InternBruker);
	}

	public String getUid() {
		return uid;
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
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("[")
                .append("identType=").append(identType).append(", ")
                .append("uid=").append(destroyed ? "destroyed" : uid)
                .append("]");
        return sb.toString();
    }
}
