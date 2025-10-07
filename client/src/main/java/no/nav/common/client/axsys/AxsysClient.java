package no.nav.common.client.axsys;

import no.nav.common.client.msgraph.AdGroupFilter;
import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;

import java.util.List;
/**
 * @deprecated Axsys skal fases ut. Vil bli erstattet med funksjonalitet i EntraId {@link no.nav.common.client.msgraph.MsGraphClient}
 */
@Deprecated(forRemoval = true)
public interface AxsysClient extends HealthCheck {
    /**
     * @deprecated kan erstattes med kall mot EntraId via {@link no.nav.common.client.msgraph.MsGraphClient#hentUserDataForGroup(String, EnhetId)} (String)}
     */
    @Deprecated(forRemoval = true)
    List<NavIdent> hentAnsatte(EnhetId enhetId);
    /**
     * @deprecated kan erstattes med kall mot EntraId via {@link no.nav.common.client.msgraph.MsGraphClient#hentAdGroupsForUser(String, String, AdGroupFilter)} (String)}
     */
    @Deprecated(forRemoval = true)
    List<AxsysEnhet> hentTilganger(NavIdent navIdent);
}
