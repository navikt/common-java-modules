package no.nav.common.client.msgraph;

import java.util.List;
import no.nav.common.types.identer.AzureObjectId;

public record UserIdResponse(List<UserData> value) {
    public record UserData(AzureObjectId id) {}
}
