package no.nav.sbl.dialogarena.time;

import java.time.LocalDate;

import static java.time.LocalDate.*;

public class NorskeHelligDagerUtil {

    public static boolean erNorskHelligDag(LocalDate dato) {
        int mnd = dato.getMonth().getValue() + 1;
        int dag = dato.getDayOfMonth();
        LocalDate paaskedag = hentFoerstePaaskedag(dato.getYear());

        if (foersteJanuaer(dag, mnd)) {
            return true;
        } else if (foesteMai(dag, mnd)) {
            return true;
        } else if (syttendeMai(dag, mnd)) {
            return true;
        } else if (foersteJuledag(dag, mnd)) {
            return true;
        } else if (andreJuledag(dag, mnd)) {
            return true;
        } else if (erPaaskeHelligdag(dato, paaskedag)) {
            return true;
        }
        return false;
    }

    private static boolean foersteJanuaer(int dag, int mnd) {
        return dag == 1 && mnd == 1;
    }

    private static boolean foesteMai(int dag, int mnd) {
        return dag == 1 && mnd == 5;
    }

    private static boolean syttendeMai(int dag, int mnd) {
        return dag == 17 && mnd == 5;
    }

    private static boolean foersteJuledag(int dag, int mnd) {
        return dag == 25 && mnd == 12;
    }

    private static boolean andreJuledag(int dag, int mnd) {
        return dag == 26 && mnd == 12;
    }

    private static boolean erPaaskeHelligdag(LocalDate dato, LocalDate paaskedag) {
        //palmesøndag
        if (dato.isEqual(paaskedag.minusDays(7))) {
            return true;
        }

        //skjærtorsdag
        if (dato.isEqual(paaskedag.minusDays(3))) {
            return true;
        }

        //langfredag
        if (dato.isEqual(paaskedag.minusDays(2))) {
            return true;
        }

        //1. påskedag
        if (dato.isEqual(paaskedag)) {
            return true;
        }

        //2. påskedag
        if (dato.isEqual(paaskedag.plusDays(1))) {
            return true;
        }

        //Kristi himmelfart
        if (dato.isEqual(paaskedag.plusDays(39))) {
            return true;
        }

        //1. pinsedag
        if (dato.isEqual(paaskedag.plusDays(49))) {
            return true;
        }

        //2. pinsedag
        if (dato.isEqual(paaskedag.plusDays(50))) {
            return true;
        }

        return false;
    }

    private static LocalDate hentFoerstePaaskedag(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = ((19 * a) + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + (2 * e) + (2 * i) - h - k) % 7;
        int m = (a + (11 - h) + (22 * l)) / 451;
        int n = (h + l - (7 * m) + 114) / 31;
        int p = (h + l - (7 * m) + 114) % 31;
        return of(year, n, p + 1);
    }
}
