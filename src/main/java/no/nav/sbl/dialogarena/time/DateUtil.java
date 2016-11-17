package no.nav.sbl.dialogarena.time;

import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.ofPattern;

public class DateUtil {

    public static String tilKortDato(LocalDate dato) {
        return dato.format(ofPattern("dd.MM.yyyy"));
    }
}
