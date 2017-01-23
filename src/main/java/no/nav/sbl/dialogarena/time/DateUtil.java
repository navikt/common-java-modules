package no.nav.sbl.dialogarena.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

import static java.time.LocalDateTime.*;
import static java.time.format.DateTimeFormatter.ofPattern;

public class DateUtil {

    public static String tilKortDato(LocalDate dato) {
        return dato.format(ofPattern("dd.MM.yyyy"));
    }

    public static String tilKortDato(LocalDateTime dato) {
        return dato.format(ofPattern("dd.MM.yyyy"));
    }

    public static String tilKortDatoMedTid(LocalDateTime dato) {
        return dato.format(ofPattern("dd.MM.yyyy HH:mm"));
    }

    public static LocalDateTime fraKortDatoMedTid(String dato) {
        return parse(dato, ofPattern("dd.MM.yyyy HH:mm"));
    }

    public static String tilKortDatoMedKlokkeslettPostfix(LocalDateTime dato) {
        return dato.format(ofPattern("dd.MM")) + " kl. " + dato.format(ofPattern("HH:mm"));
    }

    public static String tilLangDatoMedKlokkeslettPostfix(LocalDateTime dato) {
        return dato.format(ofPattern("dd.MM.yyyy")) + " kl. " + dato.format(ofPattern("HH:mm"));
    }

    public static String tilLangDatoMedKlokkeslettPostfixDagPrefix(LocalDateTime dato) {
        return dag(dato.getDayOfWeek()) + " " + dato.format(ofPattern("dd.MM.yyyy")) + " kl. " + dato.format(ofPattern("HH:mm"));
    }

    private static String dag(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return "Mandag";
            case TUESDAY:
                return "Tirsdag";
            case WEDNESDAY:
                return "Onsdag";
            case THURSDAY:
                return "Torsdag";
            case FRIDAY:
                return "Fredag";
            case SATURDAY:
                return "Lørdag";
            case SUNDAY:
                return "Søndag";
        }
        return "";
    }
}
