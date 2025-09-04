package no.nav.common.client.msgraph;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.AzureObjectId;

@Data
@Accessors(chain = true)
public class AdGroupData {
    AzureObjectId id; // uuid
    String displayName; // Nordmann, Ola
}

