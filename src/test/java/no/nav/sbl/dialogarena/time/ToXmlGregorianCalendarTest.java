package no.nav.sbl.dialogarena.time;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ToXmlGregorianCalendarTest {

    @Test
    public void fromDateTime() {
        XMLGregorianCalendar xmldate = ToXmlGregorianCalendar.FROM_DATETIME.transform(new DateTime(2013, 1, 10, 14, 30, DateTimeZone.forOffsetHours(1)));
        assertThat(xmldate.getYear(), is(2013));
        assertThat(xmldate.getMonth(), is(1));
        assertThat(xmldate.getDay(), is(10));
        assertThat(xmldate.getHour(), is(14));
        assertThat(xmldate.getMinute(), is(30));
        assertThat(xmldate.getSecond(), is(0));
        assertThat("TimeZone +1 (60 mins)", xmldate.getTimezone(), is(60));
    }

    @Test
    public void fromLocalDate() {
        XMLGregorianCalendar xmldate = ToXmlGregorianCalendar.FROM_LOCALDATE.transform(new LocalDate(2013, 1, 10));
        assertThat(xmldate.getYear(), is(2013));
        assertThat(xmldate.getMonth(), is(1));
        assertThat(xmldate.getDay(), is(10));
        assertThat(xmldate.getHour(), is(0));
        assertThat(xmldate.getMinute(), is(0));
        assertThat(xmldate.getSecond(), is(0));
    }
}
