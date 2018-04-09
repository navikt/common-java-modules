package no.nav.sbl.dialogarena.time;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.base.AbstractDateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import java.util.function.Function;

public final class ToXmlGregorianCalendar {

    private static final DatatypeFactory DATATYPES;

    static {
        try {
            DATATYPES = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static final Function<AbstractDateTime, XMLGregorianCalendar> FROM_DATETIME = datetime -> DATATYPES.newXMLGregorianCalendar(datetime.toGregorianCalendar());

    public static final Function<LocalDate, DateTime> TO_DATETIME_AT_STARTOFDAY = date -> date.toDateTimeAtStartOfDay();

    public static final Function<LocalDate, DateTime> FROM_LOCALDATE = localDate ->  TO_DATETIME_AT_STARTOFDAY.apply(localDate);

    private ToXmlGregorianCalendar() { }
}
