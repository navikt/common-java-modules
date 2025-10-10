package no.nav.common.client.msgraph;

import no.nav.common.types.identer.AzureObjectId;

public record AdGroupData(
        AzureObjectId id, // uuid
        String displayName // Nordmann, Ola
) {
}
