package no.nav.common.client.axsys;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.NavIdent;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class AxsysEnhetBruker {
    private NavIdent appIdent;
    private String historiskIdent;
}
