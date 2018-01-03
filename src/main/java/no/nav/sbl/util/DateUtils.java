package no.nav.sbl.util;

import lombok.SneakyThrows;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.util.Optional.ofNullable;

public class DateUtils {
    private static final DatatypeFactory datatypeFactory = getDatatypeFactory();

    @SneakyThrows
    private static DatatypeFactory getDatatypeFactory() {
        return DatatypeFactory.newInstance();
    }

    public static XMLGregorianCalendar getXmlCalendar(Date date) {
        return ofNullable(date).map(d->{
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            return datatypeFactory.newXMLGregorianCalendar(cal);
        }).orElse(null);
    }

    public static Date getDate(XMLGregorianCalendar xmlGregorianCalendar){
        return ofNullable(xmlGregorianCalendar)
            .map(XMLGregorianCalendar::toGregorianCalendar)
            .map(GregorianCalendar::getTime)
            .orElse(null);
    }

    public static Timestamp getTimestamp(XMLGregorianCalendar xmlGregorianCalendar) {
        Date date = getDate(xmlGregorianCalendar);
        return new Timestamp(date.getTime());
    }
}
