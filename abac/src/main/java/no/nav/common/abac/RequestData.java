package no.nav.common.abac;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.ResourceType;
import no.nav.common.abac.domain.request.ActionId;

@Getter
@Wither
@EqualsAndHashCode
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class RequestData {

    private String oidcToken;
    private String samlToken;
    private String subjectId;
    private String domain;
    private AbacPersonId personId;
    private String enhet;
    private ActionId action = ActionId.READ;
    private ResourceType resourceType;
    private String credentialResource;
}
