package no.nav.common.sts;

import org.junit.Test;

import static org.mockito.Mockito.*;

public class AzureAdServiceTokenProviderTest {

    @Test
    public void skal_lage_scope_med_default_verdier() {
        ScopedTokenProvider scopedTokenProvider = mock(ScopedTokenProvider.class);

        AzureAdServiceTokenProvider serviceTokenProvider =
                new AzureAdServiceTokenProvider("cluster", "namespace", scopedTokenProvider);

        serviceTokenProvider.getServiceToken("my-app");

        verify(scopedTokenProvider, times(1)).getToken("api://my-app.namespace.cluster/.default");
    }

    @Test
    public void skal_lage_scope_med_spesifisierte_verdier() {
        ScopedTokenProvider scopedTokenProvider = mock(ScopedTokenProvider.class);

        AzureAdServiceTokenProvider serviceTokenProvider =
                new AzureAdServiceTokenProvider("cluster", "namespace", scopedTokenProvider);

        serviceTokenProvider.getServiceToken("my-app", "test-namespace", "test-cluster");

        verify(scopedTokenProvider, times(1)).getToken("api://test-cluster.test-namespace.my-app/.default");
    }

}
