package no.nav.common.abac;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AbacCachedClientTest {

    @Test
    public void sendRequest__skal_cache_requests() {
        AbacClient client = mock(AbacClient.class);

        when(client.sendRequest(anyString())).thenReturn("response_json");

        AbacCachedClient cachedClient = new AbacCachedClient(client);

        cachedClient.sendRequest("request_json");
        cachedClient.sendRequest("request_json");

        verify(client, times(1)).sendRequest(anyString());
    }

    @Test
    public void sendRequest__skal_ikke_hente_fra_cache_for_forskjellige_requests() {
        AbacClient client = mock(AbacClient.class);

        when(client.sendRequest(anyString())).thenReturn("response_json");

        AbacCachedClient cachedClient = new AbacCachedClient(client);

        cachedClient.sendRequest("request_json1");
        cachedClient.sendRequest("request_json2");

        verify(client, times(2)).sendRequest(anyString());
    }

}
