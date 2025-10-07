package no.nav.common.client.msgraph;

import no.nav.common.types.identer.AzureObjectId;

import java.util.List;

public record UserIdResponse(
        List<UserData> value
) {
    public record UserData(AzureObjectId id) {
    }
}

