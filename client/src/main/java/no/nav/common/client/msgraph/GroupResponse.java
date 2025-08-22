package no.nav.common.client.msgraph;

import java.util.List;

public record GroupResponse(
        List<UserData> value
) {
}
