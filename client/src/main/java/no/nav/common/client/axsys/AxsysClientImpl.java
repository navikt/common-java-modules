package no.nav.common.client.axsys;

import lombok.SneakyThrows;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.UrlUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.stream.Collectors;

public class AxsysClientImpl implements AxsysClient {
    private final OkHttpClient client  = RestClient.baseClient();
    private final String axsysUrl;


    public AxsysClientImpl(String axsysUrl) {
        this.axsysUrl = axsysUrl;
    }

    @SneakyThrows
    public List<NavIdent> hentAnsatte(EnhetId enhetId) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(axsysUrl, "api/v1/enhet/"+ enhetId +"/brukere"))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        Response response = client.newCall(request).execute();
        RestUtils.throwIfNotSuccessful(response);
        List<AxsysEnhetBruker> brukere = RestUtils.parseJsonResponseArrayOrThrow(response, AxsysEnhetBruker.class);
        return brukere.stream().map(AxsysEnhetBruker::getAppIdent).collect(Collectors.toList());
    }

    @SneakyThrows
    public AxsysEnheter hentTilganger(NavIdent veileder) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(axsysUrl, "api/v1/tilgang/"+ veileder.get()))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        Response response = client.newCall(request).execute();
        RestUtils.throwIfNotSuccessful(response);
        return RestUtils.parseJsonResponseOrThrow(response, AxsysEnheter.class);
    }
}
