package no.nav.sbl.dialogarena.time;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.time.format.DateTimeFormatter.ofPattern;

public class DateUtil {

    public static String tilKortDato(LocalDate dato) {
        return dato.format(ofPattern("dd.MM.yyyy"));
    }
    public static String tilKortDato(LocalDateTime dato) {
        return dato.format(ofPattern("dd.MM.yyyy"));
    }

    public static String tilKortDatoMedTid(LocalDateTime dato){
        return dato.format(ofPattern("dd.MM.yyyy HH:mm"));
    }
}
