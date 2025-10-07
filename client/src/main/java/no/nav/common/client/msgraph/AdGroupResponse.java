package no.nav.common.client.msgraph;

import java.util.List;

public record AdGroupResponse(
        List<AdGroupData> value
) {
}
