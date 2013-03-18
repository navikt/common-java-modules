package no.nav.sbl.dialogarena.time;

import org.joda.time.DateTime;

/**
 * System clock, used in production to retrieve actual time.
 */
public class SystemClock implements Clock {

    @Override
    public DateTime now() {
        return DateTime.now();
    }

}
