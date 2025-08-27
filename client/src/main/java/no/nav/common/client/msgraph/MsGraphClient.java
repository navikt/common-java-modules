package no.nav.common.client.msgraph;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.EnhetId;

import java.util.List;

/**
 * Klient som henter ut data fra Microsoft til Graph API.
 * Se https://docs.microsoft.com/en-us/graph/overview for mer informasjon.
 * Kan også testes ut med egen bruker: https://developer.microsoft.com/en-us/graph/graph-explorer.
 * URLen til APIet er pr dags dato: https://graph.microsoft.com/v1.0.
 */
public interface MsGraphClient extends HealthCheck {

    /**
     * Henter informasjon om brukeren.
     * @param userAccessToken Brukeren sitt access token
     * @return Informasjon om brukeren
     */
    UserData hentUserData(String userAccessToken);

    List<UserData> hentUserDataForGroup(String userAccessToken, String groupId);
    List<UserData> hentUserDataForGroup(String accessToken, EnhetId enhetId);

    /**
     * Henter OnPremisesSamAccountName til brukeren.
     * @param userAccessToken Brukeren sitt access token
     * @return Hvis access tokenet tilhører en NAV-ansatt, så vil dette tilsvare NAV identen til brukeren.
     */
    String hentOnPremisesSamAccountName(String userAccessToken);

    String hentAzureGroupId(String accessToken, EnhetId enhetId);
}
