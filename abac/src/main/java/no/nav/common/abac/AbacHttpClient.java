package no.nav.common.abac;

import no.nav.common.abac.exception.AbacException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbacHttpClient implements AbacClient {

    private final static Logger LOG = LoggerFactory.getLogger(AbacHttpClient.class);

    private final String abacUrl;

    private final String srvUsername;

    private final String srvPassword;

    private final OkHttpClient client;

    public AbacHttpClient(String abacUrl, String srvUsername, String srvPassword) {
        this.abacUrl = abacUrl;
        this.srvUsername = srvUsername;
        this.srvPassword = srvPassword;
        this.client = new OkHttpClient();
    }

    @Override
    public String sendRequest(String requestXacmlJson) {
        Request request = new Request.Builder()
                .url(abacUrl)
                .addHeader("Authorization", Credentials.basic(srvUsername, srvPassword))
                .post(RequestBody.create(MediaType.get("application/xacml+json"), requestXacmlJson))
                .build();

        try(Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("ABAC returned: " +  response.code() + " " + response.message());
                throw new AbacException("An error has occured calling ABAC: " +  response.code());
            }

            return response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AbacException(e);
        }
    }

}
