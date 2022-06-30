package no.nav.common.audit_log.cef;

/**
 * Common extension fields for CEF messages
 */
public class CefMessageExtensionFields {

    /**
     * CEF-navn: sourceUserId
     * Beskrivelse: Brukeren som utfører en operasjon, kan f.eks være veileder som gjør oppslag på en bruker
     * Eksempel: Z12345
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
     * Beskrivelse: Epoch tidstempel for når slutten på eventet oppstod
     * Eksempel: 1654252356153
     */
    public final static String FIELD_END_TIME = "end";

    /**
     * CEF-navn: sourceProcessName
     * Beskrivelse: Id som identifiserer operasjonen, dette kan f.eks være Nav-Call-Id HTTP-headeren som brukes for tracing
     * Eksempel: 7b3a4c34-1b0c-40fb-bf15-743f3612c350
     */
    public final static String FIELD_SPROC = "sproc";

    /**
     * NB: Beskrivelse og eksempel er hentet fra CEF dokumentasjon
     * CEF-navn: destinationProcessName
     * Beskrivelse: The name of the event's destination process
     * Eksempel: "telnetd" or "sshd"
     */
    public final static String FIELD_DPROC = "dproc";

    /**
     * CEF-navn: requestUrl
     * Beskrivelse: URLen til HTTP-requestet som trigget eventet
     * Eksempel: /api/sak
     */
    public final static String FIELD_REQUEST = "request";

    /**
     * CEF-navn: requestMethod
     * Beskrivelse: Metoden til HTTP-requestet som trigget eventet
     * Eksempel: POST
     */
    public final static String FIELD_REQUEST_METHOD = "requestMethod";

}
