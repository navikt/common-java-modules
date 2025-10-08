package no.nav.common.client.msgraph;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.utils.AuthUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;

public class CachedMsGraphClient implements MsGraphClient {

    private final MsGraphClient msGraphClient;

    private final Cache<String, UserData> hentUserDataCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    private final Cache<String, String> hentOnPremisesSamAccountNameCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(20_000)
            .build();

    private final Cache<String, List<UserData>> hentUserDataForGroupCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    private final Cache<EnhetId, String> hentAzureGroupIdCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    private final Cache<String, List<AdGroupData>> hentAdGroupsForUserCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    private final Cache<String, List<AdGroupData>> hentAdGroupsForUserFilteredCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    private final Cache<String, String> hentAzureIdMedNavIdentCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(20_000)
            .build();

    public CachedMsGraphClient(MsGraphClient msGraphClient) {
        this.msGraphClient = msGraphClient;
    }

    @SneakyThrows
    @Override
    public UserData hentUserData(String userAccessToken) {
        String cacheKey = JWTParser.parse(userAccessToken).getJWTClaimsSet().getSubject();
        return tryCacheFirst(hentUserDataCache, cacheKey, () -> msGraphClient.hentUserData(userAccessToken));
    }

    @SneakyThrows
    @Override
    public String hentOnPremisesSamAccountName(String userAccessToken) {
        String cacheKey = JWTParser.parse(userAccessToken).getJWTClaimsSet().getSubject();
        return tryCacheFirst(hentOnPremisesSamAccountNameCache, cacheKey, () -> msGraphClient.hentOnPremisesSamAccountName(userAccessToken));
    }


    @SneakyThrows
    @Override
    public String hentAzureGroupId(String accessToken, EnhetId enhetId) {
        return tryCacheFirst(hentAzureGroupIdCache, enhetId, () -> msGraphClient.hentAzureGroupId(accessToken, enhetId));
    }

    @Override
    public String hentAzureIdMedNavIdent(String accessToken, String navIdent) {
        return tryCacheFirst(hentAzureIdMedNavIdentCache, navIdent, () -> msGraphClient.hentAzureIdMedNavIdent(accessToken, navIdent));
    }

    @SneakyThrows
    @Override
    public List<UserData> hentUserDataForGroup(String accessToken, String groupId) {
        return tryCacheFirst(hentUserDataForGroupCache, groupId, () -> msGraphClient.hentUserDataForGroup(accessToken, groupId));
    }

    @SneakyThrows
    @Override
    public List<UserData> hentUserDataForGroup(String accessToken, EnhetId enhetId) {
        String groupId = hentAzureGroupId(accessToken, enhetId);

        if (groupId == null) {
            throw new RuntimeException(format("Fant ingen groupId for enhet %s", enhetId));
        }

        return tryCacheFirst(hentUserDataForGroupCache, groupId, () -> msGraphClient.hentUserDataForGroup(accessToken, groupId));
    }

    @SneakyThrows
    @Override
    public List<AdGroupData> hentAdGroupsForUser(String accessToken, String azureAdObjectId) {
        return tryCacheFirst(hentAdGroupsForUserCache, azureAdObjectId, () -> msGraphClient.hentAdGroupsForUser(accessToken, azureAdObjectId));
    }

    @Override
    public List<AdGroupData> hentAdGroupsForUser(String accessToken, String navIdent, AdGroupFilter filter) {
        String cacheKey = navIdent + "_" + filter;
        return tryCacheFirst(hentAdGroupsForUserFilteredCache, cacheKey, () -> msGraphClient.hentAdGroupsForUser(accessToken, navIdent, filter));
    }

    @SneakyThrows
    @Override
    public List<AdGroupData> hentAdGroupsForUser(String accessToken, AdGroupFilter filter) {
        String cacheKey = JWTParser.parse(accessToken).getJWTClaimsSet().getStringClaim("NAVident") + "_" + filter;
        return tryCacheFirst(hentAdGroupsForUserFilteredCache, cacheKey, () -> msGraphClient.hentAdGroupsForUser(accessToken, filter));
    }

    @Override
    public HealthCheckResult checkHealth() {
        return msGraphClient.checkHealth();
    }

}
