package no.nav.common.auth.utils;

public class IdentUtils {

    /**
     * Sjekker om input er en gyldig NAV ident.
     * En gydlig NAV-ident regnes som en streng som starter med en upper case bokstav og deretter kun 6 siffer. Eks. Z123456
     * @param navIdentStr NAV ident som skal sjekkes
     * @return true hvis input er en gyldig NAV ident ellers false
     */
    public static boolean erGydligNavIdent(String navIdentStr) {
        if (navIdentStr == null) return false;
        return navIdentStr.matches("^[A-Z]\\d{6}$");
    }

}
