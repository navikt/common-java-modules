package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;

@Getter
@Setter
@Wither
@Accessors(chain = true)
@AllArgsConstructor
public class RequestData {

    private String oidcToken;
    private String samlToken;
    private String subjectId;
    private String domain;
    private String fnr;
    private ResourceType resourceType;
    private String credentialResource;

    RequestData() {}

}
