package no.nav.common.client.norg2;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class CachedNorg2ClientTest {

    NorgHttp2Client client;
    CachedNorg2Client cachedClient;

    @Before
    public void setup() {
        client = mock(NorgHttp2Client.class);
        cachedClient = new CachedNorg2Client(client);
    }
    @Test
    public void hentTilhorendeEnhet__bruker_cache() {
        when(client.hentTilhorendeEnhet("A", Norg2Client.Diskresjonskode.SPFO, true)).thenReturn(new Enhet());

        cachedClient.hentTilhorendeEnhet("A", Norg2Client.Diskresjonskode.SPFO, true);
        cachedClient.hentTilhorendeEnhet("A", Norg2Client.Diskresjonskode.SPFO, true);

        verify(client, times(1)).hentTilhorendeEnhet("A", Norg2Client.Diskresjonskode.SPFO, true);
    }

    @Test
    public void hentTilhorendeEnhet__bruker_alle_parameter_i_cache_key() {
        when(client.hentTilhorendeEnhet("A", Norg2Client.Diskresjonskode.SPFO, true)).thenReturn(new Enhet());
        when(client.hentTilhorendeEnhet("A", Norg2Client.Diskresjonskode.SPFO, false)).thenReturn(new Enhet());
        when(client.hentTilhorendeEnhet("A", Norg2Client.Diskresjonskode.SPSF, false)).thenReturn(new Enhet());
        when(client.hentTilhorendeEnhet("A", null, false)).thenReturn(new Enhet());
        when(client.hentTilhorendeEnhet("B", null, false)).thenReturn(new Enhet());

        cachedClient.hentTilhorendeEnhet("A", Norg2Client.Diskresjonskode.SPFO, true);
        cachedClient.hentTilhorendeEnhet("A", Norg2Client.Diskresjonskode.SPFO, false);
        cachedClient.hentTilhorendeEnhet("A", Norg2Client.Diskresjonskode.SPSF, false);
        cachedClient.hentTilhorendeEnhet("A", null, false);
        cachedClient.hentTilhorendeEnhet("B", null, false);

        verify(client, times(5)).hentTilhorendeEnhet(anyString(), any(), anyBoolean());
    }
}
