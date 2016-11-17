package no.nav.sbl.dialogarena.time;

import java.time.LocalDate;

public class HelgedagUtil {

    public static boolean erHelgedag(LocalDate dato) {
        return dato.getDayOfWeek().getValue() == 6 || dato.getDayOfWeek().getValue() == 7;
    }
}
