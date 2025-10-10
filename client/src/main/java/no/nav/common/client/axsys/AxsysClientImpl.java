package no.nav.common.client.axsys;

import okhttp3.OkHttpClient;
/**
 * @deprecated Axsys skal fases ut. Vil bli erstattet med funksjonalitet i EntraId {@link no.nav.common.client.msgraph.MsGraphClient}
 */
@Deprecated(forRemoval = true)
public class AxsysClientImpl extends BaseAxsysClient {
    /**
     * @deprecated Axsys skal fases ut. Vil bli erstattet med funksjonalitet i EntraId {@link no.nav.common.client.msgraph.MsGraphClient}
     */
    @Deprecated(forRemoval = true)
    public AxsysClientImpl(String axsysUrl) {
        super(axsysUrl, AxsysApi.V1, null);
    }
    /**
     * @deprecated Axsys skal fases ut. Vil bli erstattet med funksjonalitet i EntraId {@link no.nav.common.client.msgraph.MsGraphClient}
     */
    @Deprecated(forRemoval = true)
    public AxsysClientImpl(String axsysUrl, OkHttpClient client) {
        super(axsysUrl, AxsysApi.V1, null, client);
    }
}
