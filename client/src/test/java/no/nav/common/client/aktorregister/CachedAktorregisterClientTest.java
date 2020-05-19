package no.nav.common.client.aktorregister;

import org.junit.Test;

import static org.mockito.Mockito.*;

public class CachedAktorregisterClientTest {

    @Test
    public void hentFnr__skal_caches() {
        AktorregisterClient aktorregisterClient = mock(AktorregisterClient.class);
        when(aktorregisterClient.hentFnr("aktor_id")).thenReturn("fnr");

        CachedAktorregisterClient cachedAktorregisterKlient = new CachedAktorregisterClient(aktorregisterClient);

        cachedAktorregisterKlient.hentFnr("aktor_id");
        cachedAktorregisterKlient.hentFnr("aktor_id");

        verify(aktorregisterClient, times(1)).hentFnr("aktor_id");
    }

    @Test
    public void hentAktorId__skal_caches() {
        AktorregisterClient aktorregisterClient = mock(AktorregisterClient.class);
        when(aktorregisterClient.hentAktorId("fnr")).thenReturn("aktor_id");

        CachedAktorregisterClient cachedAktorregisterKlient = new CachedAktorregisterClient(aktorregisterClient);

        cachedAktorregisterKlient.hentAktorId("fnr");
        cachedAktorregisterKlient.hentAktorId("fnr");

        verify(aktorregisterClient, times(1)).hentAktorId("fnr");
    }

}
