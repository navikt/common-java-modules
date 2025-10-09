package no.nav.common.client.msgraph;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.nav.common.types.identer.AzureObjectId;
import no.nav.common.types.identer.EnhetId;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class CachedMsGraphClientTest {

    @Test
    public void hentUserData_skal_cache_flere_brukere() {
        String tokenStr1 = createJwtToken("user1");
        String tokenStr2 = createJwtToken("user2");

        UserData userData1 = new UserData().setId("1");
        UserData userData2 = new UserData().setId("2");

        MsGraphClient graphClient = mock(MsGraphClient.class);

        when(graphClient.hentUserData(tokenStr1)).thenReturn(userData1);
        when(graphClient.hentUserData(tokenStr2)).thenReturn(userData2);

        CachedMsGraphClient cachedMsGraphClient = new CachedMsGraphClient(graphClient);

        assertEquals(userData1, cachedMsGraphClient.hentUserData(tokenStr1));
        assertEquals(userData2, cachedMsGraphClient.hentUserData(tokenStr2));
        assertEquals(userData1, cachedMsGraphClient.hentUserData(tokenStr1));

        verify(graphClient, times(1)).hentUserData(tokenStr1);
        verify(graphClient, times(1)).hentUserData(tokenStr2);
    }

    @Test
    public void hentOnPremisesSamAccountName_skal_cache_flere_brukere() {
        String tokenStr1 = createJwtToken("user1");
        String tokenStr2 = createJwtToken("user2");

        String onPrem1 = "Z1";
        String onPrem2 = "Z2";

        MsGraphClient graphClient = mock(MsGraphClient.class);

        when(graphClient.hentOnPremisesSamAccountName(tokenStr1)).thenReturn(onPrem1);
        when(graphClient.hentOnPremisesSamAccountName(tokenStr2)).thenReturn(onPrem2);

        CachedMsGraphClient cachedMsGraphClient = new CachedMsGraphClient(graphClient);

        assertEquals(onPrem1, cachedMsGraphClient.hentOnPremisesSamAccountName(tokenStr1));
        assertEquals(onPrem2, cachedMsGraphClient.hentOnPremisesSamAccountName(tokenStr2));
        assertEquals(onPrem1, cachedMsGraphClient.hentOnPremisesSamAccountName(tokenStr1));

        verify(graphClient, times(1)).hentOnPremisesSamAccountName(tokenStr1);
        verify(graphClient, times(1)).hentOnPremisesSamAccountName(tokenStr2);
    }

    @Test
    public void hentUserDataForGroup_skal_cache_flere_brukerne_i_gruppa() {
        String tokenStr = createJwtToken("user1");
        String groupId1 = "group1";

        UserData userData1 = new UserData().setId("1");
        UserData userData2 = new UserData().setId("2");

        List<UserData> brukere = List.of(userData1, userData2);

        MsGraphClient graphClient = mock(MsGraphClient.class);

        when(graphClient.hentUserDataForGroup(tokenStr, groupId1)).thenReturn(brukere);

        CachedMsGraphClient cachedMsGraphClient = new CachedMsGraphClient(graphClient);

        assertEquals(brukere, cachedMsGraphClient.hentUserDataForGroup(tokenStr, groupId1));

        verify(graphClient, times(1)).hentUserDataForGroup(tokenStr, groupId1);
    }

    private String createJwtToken(String subject) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .build();

        return new PlainJWT(claimsSet).serialize();
    }

    @Test
    public void hentUserDataForGroup_skal_kaste_exception_naar_groupId_er_null() {
        // Arrange
        String tokenStr = createJwtToken("user1");
        EnhetId enhetId = new EnhetId("1234");

        MsGraphClient graphClient = mock(MsGraphClient.class);
        when(graphClient.hentAzureGroupId(tokenStr, enhetId)).thenReturn(null);

        CachedMsGraphClient cachedMsGraphClient = new CachedMsGraphClient(graphClient);

        // Act & Assert
        try {
            cachedMsGraphClient.hentUserDataForGroup(tokenStr, enhetId);
            fail("Expected RuntimeException was not thrown");
        } catch (RuntimeException e) {
            assertEquals("Fant ingen groupId for enhet " + enhetId, e.getMessage());
        }

        // Verify
        verify(graphClient, times(1)).hentAzureGroupId(tokenStr, enhetId);
        verify(graphClient, never()).hentUserDataForGroup(anyString(), anyString());
    }

    @Test
    public void hentAdGroupsForUser_skal_cache_grupper_for_bruker() {
        // Arrange
        String tokenStr = createJwtToken("user1");
        String azureId = "azure-id-123";

        AdGroupData group1 = new AdGroupData(new AzureObjectId("group-1"), ("Group 1"));
        AdGroupData group2 = new AdGroupData(new AzureObjectId("group-2"), ("Group 2"));
        List<AdGroupData> adGroups = List.of(group1, group2);

        MsGraphClient graphClient = mock(MsGraphClient.class);
        when(graphClient.hentAdGroupsForUser(tokenStr, azureId)).thenReturn(adGroups);

        CachedMsGraphClient cachedMsGraphClient = new CachedMsGraphClient(graphClient);

        // Act & Assert
        assertEquals(adGroups, cachedMsGraphClient.hentAdGroupsForUser(tokenStr, azureId));
        assertEquals(adGroups, cachedMsGraphClient.hentAdGroupsForUser(tokenStr, azureId));

        // Verify
        verify(graphClient, times(1)).hentAdGroupsForUser(tokenStr, azureId);
    }

    @Test
    public void hentAzureIdMedNavIdent_skal_cache_flere_brukere() {
        // Arrange
        String tokenStr = createJwtToken("user1");
        String navIdent1 = "A123456";
        String navIdent2 = "B654321";

        String azureId1 = "azure-id-1";
        String azureId2 = "azure-id-2";

        MsGraphClient graphClient = mock(MsGraphClient.class);
        when(graphClient.hentAzureIdMedNavIdent(tokenStr, navIdent1)).thenReturn(azureId1);
        when(graphClient.hentAzureIdMedNavIdent(tokenStr, navIdent2)).thenReturn(azureId2);

        CachedMsGraphClient cachedMsGraphClient = new CachedMsGraphClient(graphClient);

        // Act & Assert
        assertEquals(azureId1, cachedMsGraphClient.hentAzureIdMedNavIdent(tokenStr, navIdent1));
        assertEquals(azureId2, cachedMsGraphClient.hentAzureIdMedNavIdent(tokenStr, navIdent2));
        assertEquals(azureId1, cachedMsGraphClient.hentAzureIdMedNavIdent(tokenStr, navIdent1));

        // Verify
        verify(graphClient, times(1)).hentAzureIdMedNavIdent(tokenStr, navIdent1);
        verify(graphClient, times(1)).hentAzureIdMedNavIdent(tokenStr, navIdent2);
    }

    @Test
    public void hentAdGroupsForUser_med_token_navident_og_filter_skal_cache_grupper_for_bruker() {
        // Arrange
        String tokenStr = createJwtToken("user1");
        String navIdent = "A123456";
        AdGroupFilter filter1 = AdGroupFilter.ENHET;
        AdGroupFilter filter2 = AdGroupFilter.TEMA; // Different filter

        AdGroupData group1 = new AdGroupData(new AzureObjectId("group-1"), "Group 1");
        AdGroupData group2 = new AdGroupData(new AzureObjectId("group-2"), "Group 2");
        List<AdGroupData> adGroups1 = List.of(group1);
        List<AdGroupData> adGroups2 = List.of(group2);

        MsGraphClient graphClient = mock(MsGraphClient.class);
        when(graphClient.hentAdGroupsForUser(tokenStr, navIdent, filter1)).thenReturn(adGroups1);
        when(graphClient.hentAdGroupsForUser(tokenStr, navIdent, filter2)).thenReturn(adGroups2);

        CachedMsGraphClient cachedMsGraphClient = new CachedMsGraphClient(graphClient);

        // Act & Assert - Test caching for same filter
        assertEquals(adGroups1, cachedMsGraphClient.hentAdGroupsForUser(tokenStr, navIdent, filter1));
        assertEquals(adGroups1, cachedMsGraphClient.hentAdGroupsForUser(tokenStr, navIdent, filter1));

        // Test different filter returns different result
        assertEquals(adGroups2, cachedMsGraphClient.hentAdGroupsForUser(tokenStr, navIdent, filter2));
        assertEquals(adGroups2, cachedMsGraphClient.hentAdGroupsForUser(tokenStr, navIdent, filter2));

        // Verify each filter was called exactly once
        verify(graphClient, times(1)).hentAdGroupsForUser(tokenStr, navIdent, filter1);
        verify(graphClient, times(1)).hentAdGroupsForUser(tokenStr, navIdent, filter2);
    }

    @Test
    public void hentAdGroupsForUser_med_token_og_filter_skal_cache_grupper_for_bruker() {
        // Arrange
        String tokenStr1 = createJwtToken("user1-token");
        String tokenStr2 = createJwtToken("user2-token");

        AdGroupData group1 = new AdGroupData(new AzureObjectId("group-1"), "Group 1");
        AdGroupData group2 = new AdGroupData(new AzureObjectId("group-2"), "Group 2");
        List<AdGroupData> adGroups1 = List.of(group1);
        List<AdGroupData> adGroups2 = List.of(group2);

        MsGraphClient graphClient = mock(MsGraphClient.class);
        when(graphClient.hentAdGroupsForUser(tokenStr1, AdGroupFilter.ENHET)).thenReturn(adGroups1);
        when(graphClient.hentAdGroupsForUser(tokenStr2, AdGroupFilter.ENHET)).thenReturn(adGroups2);

        CachedMsGraphClient cachedMsGraphClient = new CachedMsGraphClient(graphClient);

        // Act & Assert - Test caching for same filter
        assertEquals(adGroups1, cachedMsGraphClient.hentAdGroupsForUser(tokenStr1, AdGroupFilter.ENHET));
        assertEquals(adGroups1, cachedMsGraphClient.hentAdGroupsForUser(tokenStr1, AdGroupFilter.ENHET));

        // Test different filter returns different result
        assertEquals(adGroups2, cachedMsGraphClient.hentAdGroupsForUser(tokenStr2, AdGroupFilter.ENHET));
        assertEquals(adGroups2, cachedMsGraphClient.hentAdGroupsForUser(tokenStr2, AdGroupFilter.ENHET));

        // Verify each filter was called exactly once
        verify(graphClient, times(1)).hentAdGroupsForUser(tokenStr1, AdGroupFilter.ENHET);
        verify(graphClient, times(1)).hentAdGroupsForUser(tokenStr2, AdGroupFilter.ENHET);
    }
}
