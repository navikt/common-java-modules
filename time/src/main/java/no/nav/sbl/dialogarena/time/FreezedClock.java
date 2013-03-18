package no.nav.sbl.dialogarena.time;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import org.joda.time.ReadableInstant;

/**
 * Freezed clock. Used in test to be able to control the time of the system.
 * This implementation <em>requires</em> a call to {@link #set(LocalDateTime)}
 * before {@link #now()} can be used to retrieve the time. During testing,
 * {@link #set(LocalDateTime)} or {@link #tick(Duration)} can be used to
 * simulate time progression.
 */
public class FreezedClock implements Clock {

    private volatile DateTime freezedTime;

    @Override
    public DateTime now() {
        if (freezedTime != null) {
            return freezedTime;
        }
        throw new IllegalStateException("Not initialized! Set the time using " + getClass().getSimpleName() + ".set(..)");
    }

    /**
     * Explicitly sets which time to return from {@link #now()}.
     */
    public void set(ReadableInstant instant) {
        this.freezedTime = new DateTime(instant);
    }

    /**
     * Relatively sets the clock incremented by the given duration. This can be used to simulate
     * "idle time" for time dependent behavior.
     * E.g the test does something, waits for some days, then does something more.
     *
     * @param duration The duration to increment the clock.
     */
    public void tick(Duration duration) {
        freezedTime = freezedTime.plus(duration);
    }

}
