package no.nav.sbl.dialogarena.time;

import org.apache.commons.collections15.Factory;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import java.util.Locale;
import java.util.function.Function;

import static org.apache.commons.collections15.FactoryUtils.constantFactory;

/**
 * Ulike tekstlige formateringer av datoer og tid, ref:
 * <a href="http://confluence.adeo.no/display/Modernisering/Datoformat">http://confluence.adeo.no/display/Modernisering/Datoformat</a>
 * <p>
 * Som default brukes {@link Locale#getDefault()} som locale for dato patterns. Bruk {@link #brukLocaleFra(Factory)} for å endre
 * hvordan locale resolves. F.eks. kan man sende inn en {@link Factory} som henter locale fra en Wicket session.
 * </p>
 * <p>
 * - Forbedringer: Utvide med støtte for internasjonalisering av datobegrepene 'i dag', 'i morgen', etc.
 * </p>
 */
public final class Datoformat {

    private static Factory<Locale> locale = constantFactory(Locale.getDefault());

    public static void brukLocaleFra(Factory<Locale> localeProvider) {
        Datoformat.locale = localeProvider;
    }

    public static void settTilbakeTilDefaultLocale() {
        locale = constantFactory(Locale.getDefault());
    }

    public static final Function<DateTime, String> KLOKKESLETT = new Formatter(new Klokkeslett());
    public static final Function<DateTime, String> LANG = new Formatter(new LangDato());
    public static final Function<DateTime, String> LANG_UTEN_LITERAL = new Formatter(new LangDatoUtenLiteral());
    public static final Function<DateTime, String> MEDIUM = new Formatter(new MediumDato());
    public static final Function<DateTime, String> KORT = new Formatter(new KortDato());
    public static final Function<DateTime, String> KORT_UTEN_LITERAL = new Formatter(new KortDatoUtenLiteral());
    public static final Function<DateTime, String> ULTRAKORT = new Formatter(new UltrakortDato());

    public static final Function<DateTime, String> LANG_MED_TID = new Formatter(new Join<>(", 'kl' ", new LangDato(), KLOKKESLETT));
    public static final Function<DateTime, String> LANG_UTEN_LITERAL_MED_TID = new Formatter(new Join<>(", 'kl' ", new LangDatoUtenLiteral(), KLOKKESLETT));
    public static final Function<DateTime, String> MEDIUM_MED_TID = new Formatter(new Join<>(", 'kl' ", new MediumDato(), KLOKKESLETT));
    public static final Function<DateTime, String> KORT_MED_TID = new Formatter(new Join<>(" 'kl' ", new KortDato(), KLOKKESLETT));


    public static String lang(DateTime dateTime) { return LANG.apply(dateTime); }
    public static String langUtenLiteral(DateTime dateTime) { return LANG_UTEN_LITERAL.apply(dateTime); }
    public static String medium(DateTime dateTime) { return MEDIUM.apply(dateTime); }
    public static String kort(DateTime dateTime) { return KORT.apply(dateTime); }
    public static String kortUtenLiteral(DateTime dateTime) { return KORT_UTEN_LITERAL.apply(dateTime); }
    public static String ultrakort(DateTime dateTime) { return ULTRAKORT.apply(dateTime); }

    public static String langMedTid(DateTime dateTime) { return LANG_MED_TID.apply(dateTime); }
    public static String langUtenLiteralMedTid(DateTime dateTime) { return LANG_UTEN_LITERAL_MED_TID.apply(dateTime); }
    public static String mediumMedTid(DateTime dateTime) { return MEDIUM_MED_TID.apply(dateTime); }
    public static String kortMedTid(DateTime dateTime) { return KORT_MED_TID.apply(dateTime); }



    public static final class LangDato extends LiteralDato {
        @Override protected String defaultPattern() { return "EEEEE d. MMMM yyyy"; }

        @Override protected String suffixPattern() { return " d. MMMM yyyy"; }
    }

    public static final class LangDatoUtenLiteral implements Function<DateTime, String> {
        @Override public String apply(DateTime dateTime) { return "d. MMMM yyyy"; }
    }

    public static final class MediumDato extends LiteralDato {
        @Override protected String defaultPattern() { return "d. MMM yyyy"; }
    }

    public static final class KortDato extends LiteralDato {
        @Override protected String defaultPattern() { return "dd.MM.yyyy"; }
    }

    public static final class UltrakortDato extends LiteralDato {
        @Override protected String defaultPattern() { return "dd.MM.yy"; }
    }

    public static final class KortDatoUtenLiteral implements Function<DateTime, String> {
        @Override public String apply(DateTime dateTime) { return "dd.MM.yyyy"; }
    }

    public static final class Klokkeslett implements Function<DateTime, String> {
        @Override public String apply(DateTime dateTime) { return "HH:mm"; }
    }


    /**
     * Transformer som gitt en dato gir tilbake et pattern for å formatere datoen. Dersom
     * datoen er idag eller nær dagens dato, vil et hensiktsmessig begrep som 'i dag' eller
     * 'i går' bli brukt.
     */
    private abstract static class LiteralDato implements Function<DateTime, String> {
        @Override
        public String apply(DateTime dateTime) {
            String literal = getDateLiteral(dateTime);
            return StringUtils.isNotBlank(literal) ? literal + suffixPattern() : defaultPattern();
        }

        protected String suffixPattern() { return ""; }

        protected abstract String defaultPattern();
    }


    /**
     * Formaterer {@link DateTime}s, gitt et {@link Locale} og dato-pattern.
     */
    private static final class Formatter implements Function<DateTime, String> {

        private final Function<? super DateTime, String> pattern;

        public Formatter(Function<? super DateTime, String> pattern) {
            this.pattern = pattern;
        }

        @Override
        public String apply(DateTime dateTime) {
            return DateTimeFormat.forPattern(pattern.apply(dateTime)).withLocale(Datoformat.locale.create()).print(dateTime);
        }

    }

    private static String getDateLiteral(DateTime dateTime) {
        LocalDate today = LocalDate.now(dateTime.getChronology());
        LocalDate date = dateTime.toLocalDate();
        if (today.equals(date)) {
            return "'i dag'";
        } else if (today.minusDays(1).equals(date)) {
            return "'i går'";
        } else if (today.plusDays(1).equals(date)) {
            return "'i morgen'";
        } else {
            return "";
        }
    }

    private Datoformat() {}
}

