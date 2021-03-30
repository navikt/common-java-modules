package no.nav.common.client.axsys;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.EnhetId;

import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class AxsysEnhet {
    EnhetId enhetId;
    String navn;
    List<String> temaer;
}