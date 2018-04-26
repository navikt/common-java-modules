package no.nav.modig.security.util;

import org.joda.time.DateTime;

public class DefaultTimeService implements TimeService {

    @Override
    public DateTime getCurrentDateTime() {
        return new DateTime();
    }
}
