package no.nav.common.client.aktorregister;

import org.junit.Test;

import static org.mockito.Mockito.*;

public class CachedAktorregisterKlientTest {

    @Test
    public void hentFnr__skal_caches() {
        AktorregisterKlient aktorregisterKlient = mock(AktorregisterKlient.class);
        when(aktorregisterKlient.hentFnr("aktor_id")).thenReturn("fnr");

        CachedAktorregisterKlient cachedAktorregisterKlient = new CachedAktorregisterKlient(aktorregisterKlient);

        cachedAktorregisterKlient.hentFnr("aktor_id");
        cachedAktorregisterKlient.hentFnr("aktor_id");

        verify(aktorregisterKlient, times(1)).hentFnr("aktor_id");
    }

    @Test
    public void hentAktorId__skal_caches() {
        AktorregisterKlient aktorregisterKlient = mock(AktorregisterKlient.class);
        when(aktorregisterKlient.hentAktorId("fnr")).thenReturn("aktor_id");

        CachedAktorregisterKlient cachedAktorregisterKlient = new CachedAktorregisterKlient(aktorregisterKlient);

        cachedAktorregisterKlient.hentAktorId("fnr");
        cachedAktorregisterKlient.hentAktorId("fnr");

        verify(aktorregisterKlient, times(1)).hentAktorId("fnr");
    }

}
