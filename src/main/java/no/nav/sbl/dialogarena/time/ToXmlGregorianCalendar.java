package no.nav.sbl.dialogarena.time;

import org.apache.commons.collections15.Transformer;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.base.AbstractDateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import static no.nav.modig.lang.collections.TransformerUtils.first;

public final class ToXmlGregorianCalendar {

    private static final DatatypeFactory DATATYPES;

    static {
        try {
            DATATYPES = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static final Transformer<AbstractDateTime, XMLGregorianCalendar> FROM_DATETIME = new Transformer<AbstractDateTime, XMLGregorianCalendar>() {
        @Override
        public XMLGregorianCalendar transform(AbstractDateTime datetime) {
            return DATATYPES.newXMLGregorianCalendar(datetime.toGregorianCalendar());
        }
    };

    public static final Transformer<LocalDate, DateTime> TO_DATETIME_AT_STARTOFDAY = new Transformer<LocalDate, DateTime>() {
        @Override
        public DateTime transform(LocalDate date) {
            return date.toDateTimeAtStartOfDay();
        }
    };

    public static final Transformer<LocalDate, XMLGregorianCalendar> FROM_LOCALDATE = first(TO_DATETIME_AT_STARTOFDAY).then(FROM_DATETIME);

    private ToXmlGregorianCalendar() { }
}
