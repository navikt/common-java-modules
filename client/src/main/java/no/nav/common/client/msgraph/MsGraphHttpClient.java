package no.nav.common.client.msgraph;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.types.identer.EnhetId;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static no.nav.common.rest.client.RestUtils.parseJsonResponseOrThrow;
import static no.nav.common.rest.client.RestUtils.throwIfNotSuccessful;
import static no.nav.common.utils.UrlUtils.joinPaths;

@Slf4j
public class MsGraphHttpClient implements MsGraphClient {

    private final static List<String> USER_DATA_FIELDS = List.of("givenName",
            "surname",
            "displayName",
            "mail",
            "onPremisesSamAccountName",
            "id");

    private final static List<String> USER_DATA_NAV_IDENT_FIELDS = Collections.singletonList("onPremisesSamAccountName");

    private final static List<String> AZURE_AD_OBJECT_ID_FIELDS = Collections.singletonList("id");

    private final static List<String> AD_GROUP_DATA_FIELDS = List.of("id", "displayName");

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
    public String hentAzureGroupId(String accessToken, EnhetId enhetId) {
        Request request = createAzureGroupIdRequest(accessToken, enhetId);

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return parseJsonResponseOrThrow(response, GroupIdResponse.class).value().getFirst().id();
        }
    }

    @SneakyThrows
    @Override
    public String hentAzureIdMedNavIdent(String accessToken, String navIdent) {
        Request request = createAzureUserIdRequest(accessToken, navIdent);
        try (Response res = client.newCall(request).execute()) {
            throwIfNotSuccessful(res);
            UserIdResponse userIdResponse = parseJsonResponseOrThrow(res, UserIdResponse.class);
            if (userIdResponse.value().isEmpty()) {
                throw new IllegalArgumentException("Fant ikke bruker i Azure AD med navIdent=" + navIdent);
            }
            if (userIdResponse.value().size() > 1) {
                log.warn("Flere enn én bruker i Azure AD med navIdent={}. Returnerer den første.", navIdent);
            }
            return userIdResponse.value().getFirst().id().toString();
        }
    }

    @SneakyThrows
    @Override
    public List<UserData> hentUserDataForGroup(String accessToken, String groupId) {
        Request request = createUsersRequest(accessToken, groupId);
        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return parseJsonResponseOrThrow(response, GroupResponse.class).value();
        }
    }

    @Override
    public List<UserData> hentUserDataForGroup(String accessToken, EnhetId enhetId) {
        String groupId = hentAzureGroupId(accessToken, enhetId);
        return hentUserDataForGroup(accessToken, groupId);
    }

    @SneakyThrows
    @Override
    public List<AdGroupData> hentAdGroupsForUser(String userAccessToken, String navIdent) {
        return hentAdGroupsForUser(userAccessToken, navIdent, null);
    }

    @SneakyThrows
    @Override
    public List<AdGroupData> hentAdGroupsForUser(String userAccessToken, String navIdent, AdGroupFilter filter) {
        String userId = hentAzureIdMedNavIdent(userAccessToken, navIdent);
        Request request = createAdGroupsForUserRequest(msGraphApiUrl, userAccessToken, userId, filter);

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return parseJsonResponseOrThrow(response, AdGroupResponse.class).value();
        }
    }


    private Request createMeRequest(String userAccessToken, List<String> fields) {
        return new Request.Builder().url(joinPaths(msGraphApiUrl, "/me") + format("?$select=%s",
                String.join(",", fields))).header("Authorization", "Bearer " + userAccessToken).build();
    }

    private Request createUsersRequest(String userAccessToken, String groupId) {
        return new Request.Builder().url(
                joinPaths(msGraphApiUrl, "/groups", groupId, "/members") + format("?$select=%s&$top=999",
                        String.join(",", MsGraphHttpClient.USER_DATA_FIELDS))
        ).header("Authorization", "Bearer " + userAccessToken).build();
    }

    private Request createAzureGroupIdRequest(String accessToken, EnhetId enhetId) {
        return new Request.Builder().url(
                joinPaths(msGraphApiUrl, "/groups") + format("?$select=id&$filter=displayName eq '0000-GA-ENHET_%s'",
                        enhetId)
        ).header("Authorization", "Bearer " + accessToken).build();
    }

    private Request createAzureUserIdRequest(String accessToken, String navIdent) {
        String encodedNavIdent = URLEncoder.encode(navIdent, StandardCharsets.UTF_8);
        return new Request.Builder().url(
                        joinPaths(msGraphApiUrl, "/users") + format(
                                "?$select=%s&$count=true&$filter=onPremisesSamAccountName eq '%s'",
                                String.join(",", AZURE_AD_OBJECT_ID_FIELDS),
                                encodedNavIdent)
                ).header("Authorization", "Bearer " + accessToken)
                .header("ConsistencyLevel", "eventual")
                .build();
    }

    public static Request createAdGroupsForUserRequest(String msGraphApiUrl, String userAccessToken, String azureAdObjectId, AdGroupFilter filter) {
        String filterQuery = (filter == null) ? null :
                "&$filter=startswith(displayName,'0000-GA-" + filter.name() + "_')";

        String queryParams = format("?$select=%s&$count=true&$top=999", String.join(",", AD_GROUP_DATA_FIELDS));
        if (filterQuery != null) {
            queryParams += filterQuery;
        }

        return new Request.Builder().url(
                        joinPaths(msGraphApiUrl,
                                "/users",
                                azureAdObjectId,
                                "memberOf")
                                + queryParams
                ).header("Authorization", "Bearer " + userAccessToken)
                .header("ConsistencyLevel", "eventual")
                .build();
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(msGraphApiUrl, client);
    }
}
