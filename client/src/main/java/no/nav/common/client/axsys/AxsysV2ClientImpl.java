package no.nav.common.client.axsys;

import okhttp3.OkHttpClient;

import java.util.function.Supplier;
/**
 * @deprecated Axsys skal fases ut. Vil bli erstattet med funksjonalitet i EntraId {@link no.nav.common.client.msgraph.MsGraphClient}
 */
@Deprecated(forRemoval = true)
public class AxsysV2ClientImpl extends BaseAxsysClient {
    /**
     * @deprecated Axsys skal fases ut. Vil bli erstattet med funksjonalitet i EntraId {@link no.nav.common.client.msgraph.MsGraphClient}
     */
    @Deprecated(forRemoval = true)
    public AxsysV2ClientImpl(String axsysUrl, Supplier<String> serviceTokenSupplier) {
        super(axsysUrl, AxsysApi.V2, serviceTokenSupplier);
    }
    /**
     * @deprecated Axsys skal fases ut. Vil bli erstattet med funksjonalitet i EntraId {@link no.nav.common.client.msgraph.MsGraphClient}
     */
    @Deprecated(forRemoval = true)
    public AxsysV2ClientImpl(String axsysUrl, Supplier<String> serviceTokenSupplier, OkHttpClient client) {
        super(axsysUrl, AxsysApi.V2, serviceTokenSupplier, client);
    }
}
