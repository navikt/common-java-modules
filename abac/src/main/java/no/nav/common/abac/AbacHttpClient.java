package no.nav.common.abac;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.abac.exception.AbacException;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import okhttp3.*;

import java.util.function.Supplier;

@Slf4j
public class AbacHttpClient implements AbacClient {

    private final String abacUrl;

    private final Supplier<String> tokenSupplier;

    private final OkHttpClient client;

    public AbacHttpClient(String abacUrl, String srvUsername, String srvPassword) {
        this.abacUrl = abacUrl;
        this.client = new OkHttpClient();
        this.tokenSupplier = () -> Credentials.basic(srvUsername, srvPassword);
    }

    public AbacHttpClient(String abacUrl, Supplier<String> tokenSupplier) {
        this.abacUrl = abacUrl;
        this.client = new OkHttpClient();
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public XacmlResponse sendRequest(XacmlRequest xacmlRequest) {
        String xacmlRequestJson = XacmlMapper.mapRequestToEntity(xacmlRequest);
        String xacmlResponseJson = sendRawRequest(xacmlRequestJson);
        return XacmlMapper.mapRawResponse(xacmlResponseJson);
    }

    @Override
    public String sendRawRequest(String xacmlRequestJson) {
        Request request = new Request.Builder()
                .url(abacUrl)
                .addHeader("Authorization", tokenSupplier.get())
                .post(RequestBody.create(MediaType.get("application/xacml+json"), xacmlRequestJson))
                .build();

        try(Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("ABAC returned unexpected status: " +  response.code() + " " + response.message());
                throw new AbacException("An error has occurred calling ABAC: " +  response.code());
            }

            return response.body().string();
        } catch (Exception e) {
            log.error("Request to ABAC failed", e);
            throw new AbacException(e);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        Request request = new Request.Builder()
                .url(abacUrl)
                .addHeader("Authorization", tokenSupplier.get())
                .build();

        return HealthCheckUtils.pingUrl(request, client);
    }
}
