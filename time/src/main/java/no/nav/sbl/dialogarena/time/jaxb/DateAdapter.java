package no.nav.sbl.dialogarena.time.jaxb;

import org.joda.time.DateMidnight;


public class DateAdapter {
    public static DateMidnight unmarshal(String v) {
        return new DateMidnight(v);
    }

    public static String marshal(DateMidnight v) {
        return v.toString();
    }
}

