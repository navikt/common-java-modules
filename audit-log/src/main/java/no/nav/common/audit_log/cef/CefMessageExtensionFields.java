package no.nav.common.audit_log.cef;

/**
 * Common extension fields for CEF messages
 */
public class CefMessageExtensionFields {

    /**
     * CEF-navn: sourceUserId
     * Beskrivelse: Brukeren som utfører en operasjon, kan f.eks være veileder som gjør oppslag på en bruker
     * Eksempel: NAV-ident
     */
    public final static String FIELD_SOURCE_USER_ID = "suid";

    /**
     * CEF-navn: destinationUserId
     * Beskrivelse: Brukeren som det blir gjort oppslag på
     * Eksempel: fnr/dnr/aktør-id
     */
    public final static String FIELD_DESTINATION_USER_ID = "duid";


    /**
     * CEF-navn: endTime
     * Beskrivelse: Når slutten på eventet oppstod, dette er som oftest bare System.currentTimeMillis()
     * Eksempel: epoch timestamp in milliseconds
     */
    public final static String FIELD_END_TIME = "end";

    /**
     * CEF-navn: sourceProcessName
     * Beskrivelse: Id som identifiserer operasjonen, dette kan f.eks være Nav-Call-Id HTTP-headeren som brukes for tracing
     * Eksempel: unik string som identifiserer operasjonen
     */
    public final static String FIELD_SPROC = "sproc";

}
