package no.nav.common.client.msgraph;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
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

        List brukere = List.of(userData1, userData2);

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

}
