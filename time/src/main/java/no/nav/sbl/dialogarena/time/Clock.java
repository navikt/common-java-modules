package no.nav.sbl.dialogarena.time;

import org.joda.time.DateTime;

/**
 * Retrieval of current date and time.
 */
public interface Clock {

    DateTime now();

}
