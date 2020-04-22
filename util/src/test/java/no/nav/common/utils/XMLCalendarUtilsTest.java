package no.nav.common.utils;

import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.util.Date;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class XMLCalendarUtilsTest {

    @Test
    public void skal_returnere_null() {
        assertThat(XMLCalendarUtils.fromDate(null), nullValue());
        assertThat(XMLCalendarUtils.toDate(null), nullValue());
        assertThat(XMLCalendarUtils.toTimestamp(null), nullValue());
    }

    @Test
    public void skal_mappe_date_til_xmlcalendar() {
        Date now = new Date();
        XMLGregorianCalendar xmlGregorianCalendar = XMLCalendarUtils.fromDate(now);
        assertThat(xmlGregorianCalendar.toGregorianCalendar().getTime(), equalTo(now));
    }

    @Test
    public void skal_mappe_xmlcalendar_til_date() {
        XMLGregorianCalendar xmlGregorianCalendar = nyXmlGregorianCalendar();

        Date date = XMLCalendarUtils.toDate(xmlGregorianCalendar);

        assertThat(date, equalTo(xmlGregorianCalendar.toGregorianCalendar().getTime()));
    }

    @Test
    public void skal_mappe_xmlcalendar_til_timestamp() {
        XMLGregorianCalendar xmlGregorianCalendar = nyXmlGregorianCalendar();

        Timestamp timestamp = XMLCalendarUtils.toTimestamp(xmlGregorianCalendar);

        assertThat(timestamp, equalTo(new Timestamp(xmlGregorianCalendar.toGregorianCalendar().getTimeInMillis())));
    }

    private XMLGregorianCalendar nyXmlGregorianCalendar() {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(2010, 6, 24, 16, 39, 11, 0, 467);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }
}