package no.nav.common.client.msgraph;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.EnhetId;

import java.util.List;

/**
 * Klient som henter ut data fra Microsoft til Graph API.
 * Se <a href="https://docs.microsoft.com/en-us/graph/overview" /> for mer informasjon.
 * Kan også testes ut med egen bruker: <a href="https://developer.microsoft.com/en-us/graph/graph-explorer" />.
 * URLen til APIet er pr dags dato: <a href="https://graph.microsoft.com/v1.0" />.
 */
public interface MsGraphClient extends HealthCheck {

    /**
     * Henter informasjon om brukeren.
     *
     * @param userAccessToken Brukeren sitt access token
     * @return Informasjon om brukeren
     */
    UserData hentUserData(String userAccessToken);

    /**
     * Henter informasjon om alle brukere som har tilgang til en AD-gruppe i Entra ID.
     * <br>
     * <br>
     * {@code accessToken} kan representere enten en autentisert bruker (OBO) eller et system (M2M), men forutsetter at riktige
     * tilganger er konfigurert. Denne operasjonen krever følgende tilganger (permissions) i Entra ID:
     * <ul>
     *     <li>{@link <a href="https://learn.microsoft.com/en-us/graph/api/group-list-members?view=graph-rest-1.0&tabs=http#permissions">List group members - Microsoft Graph v1.0 | Microsoft Learn</a>})</li>
     * </ul>
     *
     * @param accessToken et access token
     * @param groupId     identifikator som identifiserer AD-gruppen i Entra ID
     */
    List<UserData> hentUserDataForGroup(String accessToken, String groupId);

    /**
     * Henter informasjon om alle brukere som har tilgang til en AD-gruppe med {@code displayName} lik {@code 0000-GA-ENHET_XXXX}
     * i Entra ID, hvor {@code XXXX} er en {@link EnhetId} (eks: "1234").
     * <br>
     * <br>
     * {@code accessToken} kan representere enten en autentisert bruker (OBO) eller et system (M2M), men forutsetter at riktige
     * tilganger er konfigurert. Denne operasjonen krever følgende tilganger (permissions) i Entra ID:
     *  <ul>
     *     <li>{@link <a href="https://learn.microsoft.com/en-us/graph/api/group-list-members?view=graph-rest-1.0&tabs=http#permissions">List group members - Microsoft Graph v1.0 | Microsoft Learn</a>})</li>
     * </ul>
     *
     * @param accessToken et access token
     * @param enhetId     id-en til en Nav-enhet
     */
    List<UserData> hentUserDataForGroup(String accessToken, EnhetId enhetId);

    /**
     * Henter alle AD-gruppene som en person gitt ved {@code navIdent} har tilgang til.
     * <br>
     * <br>
     * {@code accessToken} kan representere enten en autentisert bruker (OBO) eller et system (M2M), men forutsetter at riktige
     * tilganger er konfigurert. Denne operasjonen krever følgende tilganger (permissions) i Entra ID:
     * <ul>
     *   <li>{@link <a href="https://learn.microsoft.com/en-us/graph/api/user-list?view=graph-rest-1.0&tabs=http#permissions">List users - Microsoft Graph v1.0 | Microsoft Learn</a>})</li>
     *   <li>{@link <a href="https://learn.microsoft.com/en-us/graph/api/user-list-memberof?view=graph-rest-1.0&tabs=http#permissions">List a user's direct memberships - Microsoft Graph v1.0 | Microsoft Learn</a>})</li>
     * </ul>
     *
     * @param accessToken et access-token
     * @param navIdent id-en til en Nav-ansatt
     */
    List<AdGroupData> hentAdGroupsForUser(String accessToken, String navIdent);

    /**
     * Henter alle AD-gruppene som en person gitt ved {@code navIdent} har tilgang til, med mulighet for filtrering.
     * <br>
     * <br>
     * {@code accessToken} kan representere enten en autentisert bruker (OBO) eller et system (M2M), men forutsetter at riktige
     * tilganger er konfigurert. Denne operasjonen krever følgende tilganger (permissions) i Entra ID:
     * <ul>
     *   <li>{@link <a href="https://learn.microsoft.com/en-us/graph/api/user-list?view=graph-rest-1.0&tabs=http#permissions">List users - Microsoft Graph v1.0 | Microsoft Learn</a>})</li>
     *   <li>{@link <a href="https://learn.microsoft.com/en-us/graph/api/user-list-memberof?view=graph-rest-1.0&tabs=http#permissions">List a user's direct memberships - Microsoft Graph v1.0 | Microsoft Learn</a>})</li>
     * </ul>
     *
     * @param accessToken et access-token
     * @param navIdent id-en til en Nav-ansatt
     * @param filter filtrerer hvilke AD-grupper som returneres basert på prefiks
     */
    List<AdGroupData> hentAdGroupsForUser(String accessToken, String navIdent, AdGroupFilter filter);

    /**
     * Henter alle AD-gruppene som innlogget bruker har tilgang til, med mulighet for filtrering.
     * <br>
     * <br>
     * {@code accessToken} representere en autentisert bruker (OBO), men forutsetter at riktige
     * tilganger er konfigurert. Denne operasjonen krever følgende tilganger (permissions) i Entra ID:
     * <ul>
     *   <li>{@link <a href="https://learn.microsoft.com/en-us/graph/api/user-list-memberof?view=graph-rest-1.0&tabs=http#permissions-for-the-signed-in-users-direct-memberships">List a user's direct memberships - Microsoft Graph v1.0 | Microsoft Learn</a>})</li>
     * </ul>
     *
     * NB: Denne operasjonen forventer at {@code accessToken} inneholder claimet "NAVident", da dette brukes ifm. caching.
     *
     * @param accessToken et access-token
     * @param filter filtrerer hvilke AD-grupper som returneres basert på prefiks
     */
    List<AdGroupData> hentAdGroupsForUser(String accessToken, AdGroupFilter filter);

    /**
     * Henter OnPremisesSamAccountName til brukeren.
     *
     * @param userAccessToken Brukeren sitt access token
     * @return Hvis access tokenet tilhører en NAV-ansatt, så vil dette tilsvare NAV identen til brukeren.
     */
    String hentOnPremisesSamAccountName(String userAccessToken);

    /**
     * Henter en unik Entra ID identifikator for AD-gruppen med {@code displayName} lik {@code 0000-GA-ENHET_XXXX},
     * hvor {@code XXXX} er en {@link EnhetId} (eks: "1234").
     * <br>
     * <br>
     * {@code accessToken} kan representere enten en autentisert bruker (OBO) eller et system (M2M), men forutsetter at riktige
     * tilganger er konfigurert. Denne operasjonen krever følgende tilganger (permissions) i Entra ID:
     * <ul>
     *   <li>{@link <a href="https://learn.microsoft.com/en-us/graph/api/group-list?view=graph-rest-1.0&tabs=http#permissions">List groups - Microsoft Graph v1.0 | Microsoft Learn</a>})</li>
     * </ul>
     *
     * @param accessToken et access-token
     * @param enhetId id-en til en Nav-enhet
     */
    String hentAzureGroupId(String accessToken, EnhetId enhetId);

    /**
     * Henter en unik Entra ID identifikator for en Nav-ansatt.
     * <br>
     * <br>
     * {@code accessToken} kan representere enten en autentisert bruker (OBO) eller et system (M2M), men forutsetter at riktige
     * tilganger er konfigurert. Denne operasjonen krever følgende tilganger (permissions) i Entra ID:
     * <ul>
     *   <li>{@link <a href="https://learn.microsoft.com/en-us/graph/api/user-list?view=graph-rest-1.0&tabs=http#permissions">List users - Microsoft Graph v1.0 | Microsoft Learn</a>})</li>
     * </ul>
     *
     * @param accessToken et access-token
     * @param navIdent id-en til en Nav-ansatt
     */
    String hentAzureIdMedNavIdent(String accessToken, String navIdent);
}
