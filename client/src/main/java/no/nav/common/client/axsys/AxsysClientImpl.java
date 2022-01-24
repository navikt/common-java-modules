package no.nav.common.client.axsys;

import okhttp3.OkHttpClient;

public class AxsysClientImpl extends BaseAxsysClient {
    public AxsysClientImpl(String axsysUrl) {
        super(axsysUrl, AxsysApi.V1, null);
    }

    public AxsysClientImpl(String axsysUrl, OkHttpClient client) {
        super(axsysUrl, AxsysApi.V1, null, client);
    }
}
