package no.nav.common.client.msgraph;

import java.util.List;

public record GroupIdResponse(
        List<GroupId> value
) {
    public record GroupId(String id) {
    }
}

