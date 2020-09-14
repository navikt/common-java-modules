package no.nav.common.types.identer;

/**
 * Representerer Azure AD IDen til en saksbehandler (Hentes fra "oid" (Object ID) claimet).
 * Eksempel: aaa123-aaa123-aaa123-aaa123-aaa123
 */
public class AzureObjectId extends InternBrukerId {

    private AzureObjectId(String id) {
        super(id);
    }

    public static AzureObjectId of(String azureAdId) {
        return new AzureObjectId(azureAdId);
    }

}
