package no.nav.common.client.msgraph;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static no.nav.common.rest.client.RestUtils.*;
import static no.nav.common.utils.UrlUtils.joinPaths;

@Slf4j
public class MsGraphHttpClient implements MsGraphClient {

    private final static List<String> USER_DATA_FIELDS = List.of("givenName", "surname", "displayName", "mail", "onPremisesSamAccountName", "id");

    private final static List<String> USER_DATA_NAV_IDENT_FIELDS = Collections.singletonList("onPremisesSamAccountName");

    private final String msGraphApiUrl;

    private final OkHttpClient client;

    public MsGraphHttpClient(String msGraphApiUrl) {
        this.msGraphApiUrl = msGraphApiUrl;
        this.client = RestClient.baseClient();
    }

    @SneakyThrows
    @Override
    public UserData hentUserData(String userAccessToken) {
        Request request = createMeRequest(userAccessToken, USER_DATA_FIELDS);

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return parseJsonResponseOrThrow(response, UserData.class);
        }
    }

    @SneakyThrows
    @Override
    public String hentOnPremisesSamAccountName(String userAccessToken) {
        Request request = createMeRequest(userAccessToken, USER_DATA_NAV_IDENT_FIELDS);

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return parseJsonResponseOrThrow(response, OnPremisesSamAccountName.class).onPremisesSamAccountName;
        }
    }

    @SneakyThrows
    @Override
    public List<UserData> hentUserDataForGroup(String userAccessToken, String groupId) {
        Request request = createUsersRequest(userAccessToken, groupId);
        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return parseJsonResponseOrThrow(response, GroupResponse.class).value();

        }
    }

    private Request createMeRequest(String userAccessToken, List<String> fields) {
        return new Request.Builder().url(joinPaths(msGraphApiUrl, "/me") + format("?$select=%s", String.join(",", fields))).header("Authorization", "Bearer " + userAccessToken).build();
    }

    private Request createUsersRequest(String userAccessToken, String groupId) {
        return new Request.Builder().url(joinPaths(msGraphApiUrl, "/groups", groupId) + format("?$select=%s", String.join(",", MsGraphHttpClient.USER_DATA_FIELDS))).header("Authorization", "Bearer " + userAccessToken).build();
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(msGraphApiUrl, client);
    }

}
