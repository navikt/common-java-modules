package no.nav.sbl.dialogarena.time;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.ReadableInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Freezed clock. Used in test to be able to control the time of the system.
 * This implementation <em>requires</em> a call to {@link #set(ReadableInstant)}
 * before {@link #now()} can be used to retrieve the time. During testing,
 * {@link #set(ReadableInstant)} or {@link #tick(Duration)} can be used to
 * simulate time progression.
 */
public class FreezedClock implements Clock {

    private static final Logger LOG = LoggerFactory.getLogger(FreezedClock.class);

    private volatile DateTime freezedTime;

    public FreezedClock() {
        LOG.warn("Using freezed clock instead of system clock! This is only appropriate for testing. " +
                "If that is the case, this warning can be disregarded.");
    }

    @Override
    public final DateTime now() {
        if (freezedTime != null) {
            return freezedTime;
        }
        throw new IllegalStateException(
                FreezedClock.class.getSimpleName() + " not initialized! Set the time using " + getClass().getSimpleName() + ".set(..). " +
                        "If this happens outside of testing in production or production-like environment, " +
                        "you have an error in your Spring configuration, and you must ensure that your application uses SystemClock instead.");
    }

    /**
     * Explicitly sets which time to return from {@link #now()}.
     */
    public final void set(ReadableInstant instant) {
        this.freezedTime = new DateTime(instant);
    }

    /**
     * Relatively sets the clock incremented by the given duration. This can be used to simulate
     * "idle time" for time dependent behavior.
     * E.g the test does something, waits for some days, then does something more.
     *
     * @param duration The duration to increment the clock.
     */
    public final void tick(Duration duration) {
        freezedTime = freezedTime.plus(duration);
    }
}