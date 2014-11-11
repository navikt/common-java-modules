package no.nav.sbl.dialogarena.time;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import java.util.Locale;

import static org.apache.commons.collections15.FactoryUtils.constantFactory;
import static org.apache.commons.collections15.TransformerUtils.constantTransformer;

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


    public static final Transformer<Object, String> TID = constantTransformer("HH:mm");

    public static final Transformer<DateTime, String> LANG = new Formatter(new LangDato());
    public static final Transformer<DateTime, String> LANG_UTEN_LITERAL = new Formatter(new LangDatoUtenLiteral());;
    public static final Transformer<DateTime, String> MEDIUM = new Formatter(new MediumDato());
    public static final Transformer<DateTime, String> KORT = new Formatter(new KortDato());
    public static final Transformer<DateTime, String> KORT_UTEN_LITERAL = new Formatter(new KortDatoUtenLiteral());
    public static final Transformer<DateTime, String> ULTRAKORT = new Formatter(new UltrakortDato());

    public static final Transformer<DateTime, String> LANG_MED_TID = new Formatter(new Join<>(", 'kl' ", new LangDato(), TID));
    public static final Transformer<DateTime, String> LANG_UTEN_LITERAL_MED_TID = new Formatter(new Join<>(", 'kl' ", new LangDatoUtenLiteral(), TID));
    public static final Transformer<DateTime, String> MEDIUM_MED_TID = new Formatter(new Join<>(", 'kl' ", new MediumDato(), TID));
    public static final Transformer<DateTime, String> KORT_MED_TID = new Formatter(new Join<>(" 'kl' ", new KortDato(), TID));



    public static String lang(DateTime dateTime) { return LANG.transform(dateTime); }
    public static String langUtenLiteral(DateTime dateTime) { return LANG_UTEN_LITERAL.transform(dateTime); }
    public static String medium(DateTime dateTime) { return MEDIUM.transform(dateTime); }
    public static String kort(DateTime dateTime) { return KORT.transform(dateTime); }
    public static String kortUtenLiteral(DateTime dateTime) { return KORT_UTEN_LITERAL.transform(dateTime); }
    public static String ultrakort(DateTime dateTime) { return ULTRAKORT.transform(dateTime); }

    public static String langMedTid(DateTime dateTime) { return LANG_MED_TID.transform(dateTime); }
    public static String langUtenLiteralMedTid(DateTime dateTime) { return LANG_UTEN_LITERAL_MED_TID.transform(dateTime); }
    public static String mediumMedTid(DateTime dateTime) { return MEDIUM_MED_TID.transform(dateTime); }
    public static String kortMedTid(DateTime dateTime) { return KORT_MED_TID.transform(dateTime); }



    public static final class LangDato extends LiteralDato {
        @Override protected String defaultPattern() { return "EEEEE d. MMMM yyyy"; }

        @Override protected String suffixPattern() { return " d. MMMM yyyy"; }
    }

    public static final class LangDatoUtenLiteral implements Transformer<DateTime, String> {
        @Override public String transform(DateTime dateTime) { return "d. MMMM yyyy"; }
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

    public static final class KortDatoUtenLiteral implements Transformer<DateTime, String> {
        @Override public String transform(DateTime dateTime) { return "dd.MM.yyyy"; }
    }


    /**
     * Transformer som gitt en dato gir tilbake et pattern for å formatere datoen. Dersom
     * datoen er idag eller nær dagens dato, vil et hensiktsmessig begrep som 'i dag' eller
     * 'i går' bli brukt.
     */
    private abstract static class LiteralDato implements Transformer<DateTime, String> {
        @Override
        public String transform(DateTime dateTime) {
            String literal = getDateLiteral(dateTime);
            return StringUtils.isNotBlank(literal) ? literal + suffixPattern() : defaultPattern();
        }

        protected String suffixPattern() { return ""; }

        protected abstract String defaultPattern();
    }


    /**
     * Formaterer {@link DateTime}s, gitt et {@link Locale} og dato-pattern.
     */
    private static final class Formatter implements Transformer<DateTime, String> {

        private final Transformer<? super DateTime, String> pattern;

        public Formatter(Transformer<? super DateTime, String> pattern) {
            this.pattern = pattern;
        }

        @Override
        public String transform(DateTime dateTime) {
            return DateTimeFormat.forPattern(pattern.transform(dateTime)).withLocale(Datoformat.locale.create()).print(dateTime);
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

