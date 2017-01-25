package no.nav.fo.security.jwt.domain;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import java.security.Principal;

public final class ConsumerId implements Principal, Destroyable {

    private String consumerIdString;
    private boolean destroyed;

    public ConsumerId(String consumerId) {
        this.consumerIdString = consumerId;
    }

    public ConsumerId() {
        consumerIdString = System.getProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);

        if (consumerIdString == null) {
            throw new IllegalStateException(
                    StsSecurityConstants.SYSTEMUSER_USERNAME + " is not set, failed to set " + ConsumerId.class.getName());
        }
    }

    @Override
    public void destroy() throws DestroyFailedException {
        consumerIdString = null;
        destroyed = true;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public String getName() {
        return consumerIdString;
    }

    public String getConsumerId() {
        return consumerIdString;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                (destroyed ? "destroyed" : consumerIdString) +
                "]";
    }
}
