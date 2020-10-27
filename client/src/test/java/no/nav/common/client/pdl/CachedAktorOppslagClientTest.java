package no.nav.common.client.pdl;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CachedAktorOppslagClientTest {

    @Test
    public void hentFnr__skal_cache_flere_fnr() {
        AktorId aktorId1 = AktorId.of("aktor1");
        AktorId aktorId2 = AktorId.of("aktor2");

        Fnr fnr1 = Fnr.of("fnr1");
        Fnr fnr2 = Fnr.of("fnr2");

        AktorOppslagClient mockedClient = mock(AktorOppslagClient.class);

        when(mockedClient.hentFnr(aktorId1)).thenReturn(fnr1);
        when(mockedClient.hentFnr(aktorId2)).thenReturn(fnr2);

        CachedAktorOppslagClient cachedClient = new CachedAktorOppslagClient(mockedClient);

        assertEquals(fnr1, cachedClient.hentFnr(aktorId1));
        assertEquals(fnr2, cachedClient.hentFnr(aktorId2));
        assertEquals(fnr1, cachedClient.hentFnr(aktorId1));

        verify(mockedClient, times(1)).hentFnr(aktorId1);
        verify(mockedClient, times(1)).hentFnr(aktorId2);
    }

    @Test
    public void hentAktorId__skal_cache_flere_aktor_ider() {
        Fnr fnr1 = Fnr.of("fnr1");
        Fnr fnr2 = Fnr.of("fnr2");

        AktorId aktorId1 = AktorId.of("aktor1");
        AktorId aktorId2 = AktorId.of("aktor2");

        AktorOppslagClient mockedClient = mock(AktorOppslagClient.class);

        when(mockedClient.hentAktorId(fnr1)).thenReturn(aktorId1);
        when(mockedClient.hentAktorId(fnr2)).thenReturn(aktorId2);

        CachedAktorOppslagClient cachedClient = new CachedAktorOppslagClient(mockedClient);

        assertEquals(aktorId1, cachedClient.hentAktorId(fnr1));
        assertEquals(aktorId2, cachedClient.hentAktorId(fnr2));
        assertEquals(aktorId1, cachedClient.hentAktorId(fnr1));

        verify(mockedClient, times(1)).hentAktorId(fnr1);
        verify(mockedClient, times(1)).hentAktorId(fnr2);
    }

    @Test
    public void hentFnrBolk__skal_cache_fnr_individuelt() {
        Fnr fnr1 = Fnr.of("fnr1");
        Fnr fnr2 = Fnr.of("fnr2");
        Fnr fnr3 = Fnr.of("fnr3");
        Fnr fnr4 = Fnr.of("fnr4");

        AktorId aktorId1 = AktorId.of("aktor1");
        AktorId aktorId2 = AktorId.of("aktor2");
        AktorId aktorId3 = AktorId.of("aktor3");
        AktorId aktorId4 = AktorId.of("aktor4");

        AktorOppslagClient mockedClient = mock(AktorOppslagClient.class);

        CachedAktorOppslagClient cachedClient = new CachedAktorOppslagClient(mockedClient);

        ArgumentCaptor<List<AktorId>> captor = ArgumentCaptor.forClass(List.class);



        Map<AktorId, Fnr> mapping1 = new HashMap<>();
        mapping1.put(aktorId1, fnr1);
        mapping1.put(aktorId2, fnr2);
        mapping1.put(aktorId3, fnr3);

        doReturn(mapping1).when(mockedClient).hentFnrBolk(captor.capture());

        List<AktorId> aktorIdList1 = List.of(aktorId1, aktorId2, aktorId3);
        Map<AktorId, Fnr> oppslag1 = cachedClient.hentFnrBolk(aktorIdList1);

        assertEquals(3, oppslag1.size());
        assertEquals(fnr1, oppslag1.get(aktorId1));
        assertEquals(fnr2, oppslag1.get(aktorId2));
        assertEquals(fnr3, oppslag1.get(aktorId3));
        assertEquals(aktorIdList1, captor.getValue());



        Map<AktorId, Fnr> mapping2 = new HashMap<>();
        mapping2.put(aktorId2, fnr2);
        mapping2.put(aktorId3, fnr3);
        mapping2.put(aktorId4, fnr4);

        doReturn(mapping2).when(mockedClient).hentFnrBolk(captor.capture());

        List<AktorId> aktorIdList2 = List.of(aktorId2, aktorId3, aktorId4);
        Map<AktorId, Fnr> oppslag2 = cachedClient.hentFnrBolk(aktorIdList2);

        assertEquals(3, oppslag2.size());
        assertEquals(fnr2, oppslag2.get(aktorId2));
        assertEquals(fnr3, oppslag2.get(aktorId3));
        assertEquals(fnr4, oppslag2.get(aktorId4));
        assertEquals(List.of(aktorId4), captor.getValue());
    }

    @Test
    public void hentAktorIdBolk__skal_cache_aktorid_individuelt() {

        AktorId aktorId1 = AktorId.of("aktor1");
        AktorId aktorId2 = AktorId.of("aktor2");
        AktorId aktorId3 = AktorId.of("aktor3");
        AktorId aktorId4 = AktorId.of("aktor4");

        Fnr fnr1 = Fnr.of("fnr1");
        Fnr fnr2 = Fnr.of("fnr2");
        Fnr fnr3 = Fnr.of("fnr3");
        Fnr fnr4 = Fnr.of("fnr4");


        AktorOppslagClient mockedClient = mock(AktorOppslagClient.class);

        CachedAktorOppslagClient cachedClient = new CachedAktorOppslagClient(mockedClient);

        ArgumentCaptor<List<Fnr>> captor = ArgumentCaptor.forClass(List.class);


        Map<Fnr, AktorId> mapping1 = new HashMap<>();
        mapping1.put(fnr1, aktorId1);
        mapping1.put(fnr2, aktorId2);
        mapping1.put(fnr3, aktorId3);

        doReturn(mapping1).when(mockedClient).hentAktorIdBolk(captor.capture());

        List<Fnr> fnrList1 = List.of(fnr1, fnr2, fnr3);
        Map<Fnr, AktorId> oppslag1 = cachedClient.hentAktorIdBolk(fnrList1);

        assertEquals(3, oppslag1.size());
        assertEquals(aktorId1, oppslag1.get(fnr1));
        assertEquals(aktorId2, oppslag1.get(fnr2));
        assertEquals(aktorId3, oppslag1.get(fnr3));
        assertEquals(fnrList1, captor.getValue());



        Map<Fnr, AktorId> mapping2 = new HashMap<>();
        mapping2.put(fnr2, aktorId2);
        mapping2.put(fnr3, aktorId3);
        mapping2.put(fnr4, aktorId4);

        doReturn(mapping2).when(mockedClient).hentAktorIdBolk(captor.capture());

        List<Fnr> fnrList2 = List.of(fnr2, fnr3, fnr4);
        Map<Fnr, AktorId> oppslag2 = cachedClient.hentAktorIdBolk(fnrList2);

        assertEquals(3, oppslag2.size());
        assertEquals(aktorId2, oppslag2.get(fnr2));
        assertEquals(aktorId3, oppslag2.get(fnr3));
        assertEquals(aktorId4, oppslag2.get(fnr4));
        assertEquals(List.of(fnr4), captor.getValue());
    }

}
