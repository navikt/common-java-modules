package no.nav.common.types.identer;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Representerer Azure AD IDen til en saksbehandler (Hentes fra "oid" (Object ID) claimet).
 * Eksempel: aaa123-aaa123-aaa123-aaa123-aaa123
 */
public class AzureObjectId extends InternBrukerId {

    @JsonCreator
    public AzureObjectId(String id) {
        super(id);
    }

    public static AzureObjectId of(String azureAdId) {
        return new AzureObjectId(azureAdId);
    }

}
