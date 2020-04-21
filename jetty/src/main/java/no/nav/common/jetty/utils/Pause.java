package no.nav.common.jetty.utils;


import org.apache.commons.collections15.Factory;

import static java.lang.Boolean.FALSE;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static org.apache.commons.collections15.FactoryUtils.constantFactory;

/**
 * Hold execution for a while based on various conditions.
 */
public final class Pause {

    public static Pause pause() {
        return new Pause();
    }

    private long timeoutDuration;
    private boolean mayTimeout = true;
    private boolean silentTimeout = false;
    private long pollInterval;

    private Pause() {
        this.timeoutDuration = 20000;
        this.pollInterval = 100;
    }

    public void forMilliseconds(long milliseconds) {
        timeoutSilently().timeout(milliseconds).until(constantFactory(FALSE));
    }

    public Pause timeout(long timeoutMs) {
        this.timeoutDuration = timeoutMs;
        return this;
    }

    public Pause timeoutSilently() {
        this.silentTimeout = true;
        return this;
    }

    public Pause noTimeout() {
        mayTimeout = false;
        return this;
    }

    public Pause pollEveryMs(long pollIntervalMs) {
        this.pollInterval = pollIntervalMs;
        return this;
    }

    public void until(Factory<Boolean> condition) {
        try {
            long startTime = currentTimeMillis();
            while (!condition.create()) {
                sleep(pollInterval);
                if (mayTimeout && currentTimeMillis() - startTime > timeoutDuration) {
                    if (silentTimeout) {
                        break;
                    } else {
                        throw new TimedOut(condition, timeoutDuration);
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static class TimedOut extends RuntimeException {
        public TimedOut(Factory<Boolean> condition, long timeoutMilliseconds) {
            super("Timed out while waiting for " + condition + " (timeout=" + timeoutMilliseconds + " ms)");
        }
    }
}
