package no.nav.common.client.axsys;

import okhttp3.OkHttpClient;

import java.util.function.Supplier;

public class AxsysV2ClientImpl extends BaseAxsysClient {
    public AxsysV2ClientImpl(String axsysUrl, Supplier<String> serviceTokenSupplier) {
        super(axsysUrl, AxsysApi.V2, serviceTokenSupplier);
    }

    public AxsysV2ClientImpl(String axsysUrl, Supplier<String> serviceTokenSupplier, OkHttpClient client) {
        super(axsysUrl, AxsysApi.V2, serviceTokenSupplier, client);
    }
}
