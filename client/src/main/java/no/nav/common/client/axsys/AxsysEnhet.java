package no.nav.common.client.axsys;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.EnhetId;

import java.util.List;
/**
 * @deprecated Axsys skal fases ut. Vil bli erstattet med funksjonalitet i EntraId {@link no.nav.common.client.msgraph.MsGraphClient}
 * NB! Datamodellen kan ikke brukes as is, fordi EntraId har ingen kobling mellom enhet og tema, eller mellom enhet og enhetsnavn.
 * Enhetsnavn kan hentes fra Norg. Tema kan hentes fra EntraId.
 */
@Deprecated(forRemoval = true)
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class AxsysEnhet {
    EnhetId enhetId;
    String navn;
    List<String> temaer;
}